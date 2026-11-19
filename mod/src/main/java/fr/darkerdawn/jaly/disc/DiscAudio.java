package fr.darkerdawn.jaly.disc;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.OggAudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Streams a song file (mp3, ogg or wav) to Minecraft's sound engine, as mono so it sounds like it comes from the jukebox. */
public final class DiscAudio {
    private DiscAudio() {}

    public static AudioStream open(Path file) throws IOException {
        byte[] head = new byte[4];
        try (InputStream in = Files.newInputStream(file)) {
            int n = in.readNBytes(head, 0, 4);
            if (n < 4) throw new IOException("empty file");
        }
        if (head[0] == 'O' && head[1] == 'g' && head[2] == 'g' && head[3] == 'S') return new Mono(new OggAudioStream(new BufferedInputStream(Files.newInputStream(file))));
        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F') return new Wav(file);
        return new Mp3(file);
    }

    /** Downmixes any 16-bit PCM stream to mono. */
    static final class Mono implements AudioStream {
        private final AudioStream in;
        private final AudioFormat src;

        Mono(AudioStream in) {
            this.in = in;
            this.src = in.getFormat();
        }

        @Override
        public AudioFormat getFormat() { return new AudioFormat(src.getSampleRate(), 16, 1, true, false); }

        @Override
        public ByteBuffer read(int size) throws IOException {
            if (src.getChannels() == 1) return in.read(size);
            ByteBuffer b = in.read(size * src.getChannels());
            if (b == null || !b.hasRemaining()) return null;
            b.order(src.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            int ch = src.getChannels(), frames = b.remaining() / (2 * ch);
            ByteBuffer out = BufferUtils.createByteBuffer(frames * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (int f = 0; f < frames; f++) {
                int sum = 0;
                for (int c = 0; c < ch; c++) sum += b.getShort();
                out.putShort((short) (sum / ch));
            }
            out.flip();
            return out;
        }

        @Override
        public void close() throws IOException { in.close(); }
    }

    /** MP3 through JLayer. */
    static final class Mp3 implements AudioStream {
        private final InputStream raw;
        private final Bitstream bits;
        private final Decoder decoder = new Decoder();
        private AudioFormat format;
        private short[] pending = new short[0];
        private int pendingPos;
        private boolean done;

        Mp3(Path file) throws IOException {
            raw = new BufferedInputStream(Files.newInputStream(file), 1 << 16);
            bits = new Bitstream(raw);
            if (!decodeNext()) throw new IOException("not an mp3 file");
        }

        private boolean decodeNext() throws IOException {
            try {
                for (int tries = 0; tries < 64; tries++) {
                    Header h = bits.readFrame();
                    if (h == null) {
                        done = true;
                        return false;
                    }
                    try {
                        SampleBuffer out = (SampleBuffer) decoder.decodeFrame(h, bits);
                        int ch = decoder.getOutputChannels(), len = out.getBufferLength();
                        short[] buf = out.getBuffer();
                        short[] mono = new short[len / ch];
                        for (int i = 0; i < mono.length; i++) {
                            int sum = 0;
                            for (int c = 0; c < ch; c++) sum += buf[i * ch + c];
                            mono[i] = (short) (sum / ch);
                        }
                        if (format == null) format = new AudioFormat(decoder.getOutputFrequency(), 16, 1, true, false);
                        pending = mono;
                        pendingPos = 0;
                        return true;
                    } finally {
                        bits.closeFrame();
                    }
                }
                return false;
            } catch (Exception e) {
                done = true;
                if (format == null) throw new IOException("can't decode this mp3", e);
                return false;
            }
        }

        @Override
        public AudioFormat getFormat() { return format; }

        @Override
        public ByteBuffer read(int size) throws IOException {
            int samples = Math.max(1, size / 2);
            ByteBuffer out = BufferUtils.createByteBuffer(samples * 2).order(ByteOrder.LITTLE_ENDIAN);
            while (out.remaining() >= 2) {
                if (pendingPos >= pending.length && (done || !decodeNext())) break;
                out.putShort(pending[pendingPos++]);
            }
            out.flip();
            return out.hasRemaining() ? out : null;
        }

        @Override
        public void close() throws IOException {
            try {
                bits.close();
            } catch (Exception ignored) {
            }
            raw.close();
        }
    }

    /** WAV through Java Sound. */
    static final class Wav implements AudioStream {
        private final AudioInputStream in;
        private final AudioFormat src;

        Wav(Path file) throws IOException {
            try {
                AudioInputStream a = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(file)));
                AudioFormat f = a.getFormat();
                AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16, f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
                in = AudioSystem.getAudioInputStream(pcm, a);
                src = pcm;
            } catch (Exception e) {
                throw new IOException("can't read this wav file", e);
            }
        }

        @Override
        public AudioFormat getFormat() { return new AudioFormat(src.getSampleRate(), 16, 1, true, false); }

        @Override
        public ByteBuffer read(int size) throws IOException {
            int ch = src.getChannels();
            byte[] buf = new byte[Math.max(2 * ch, (size / 2) * 2 * ch)];
            int n = in.readNBytes(buf, 0, buf.length);
            if (n <= 0) return null;
            ByteBuffer b = ByteBuffer.wrap(buf, 0, n).order(ByteOrder.LITTLE_ENDIAN);
            int frames = n / (2 * ch);
            ByteBuffer out = BufferUtils.createByteBuffer(frames * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (int f = 0; f < frames; f++) {
                int sum = 0;
                for (int c = 0; c < ch; c++) sum += b.getShort();
                out.putShort((short) (sum / ch));
            }
            out.flip();
            return out;
        }

        @Override
        public void close() throws IOException { in.close(); }
    }
}
