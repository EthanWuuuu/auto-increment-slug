package cn.ethanwu.autoincrementslug.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class AISConfigService {

    private static final long CUSTOM_EPOCH = 1577808000000L;
    private static final int SEQUENCE_BITS = 8;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final int ID_BITS = 40;
    private static final long ID_MASK = (1L << ID_BITS) - 1;
    private static final long OBFUSCATION_KEY = 0x1d5a1c2e9L;
    private static final int SLUG_LENGTH = 12;

    private final AtomicLong lastTimestamp = new AtomicLong(0L);
    private final AtomicInteger sequence = new AtomicInteger(0);

    public String generateSlug() {
        while (true) {
            long currentUnit = (System.currentTimeMillis() - CUSTOM_EPOCH) / 100L;
            if (currentUnit < 0) {
                currentUnit = 0;
            }

            long last = lastTimestamp.get();
            if (currentUnit < last) {
                currentUnit = last;
            }

            if (currentUnit == last) {
                int seq = sequence.incrementAndGet();
                if ((seq & SEQUENCE_MASK) != seq) {
                    currentUnit = waitNextMillis(last);
                    if (lastTimestamp.compareAndSet(last, currentUnit)) {
                        sequence.set(0);
                    }
                    continue;
                }
                long id = (currentUnit << SEQUENCE_BITS) | (seq & SEQUENCE_MASK);
                return encodeId(id);
            } else {
                if (lastTimestamp.compareAndSet(last, currentUnit)) {
                    sequence.set(0);
                    long id = currentUnit << SEQUENCE_BITS;
                    return encodeId(id);
                }
            }
        }
    }

    private long waitNextMillis(long lastMillis) {
        long current;
        do {
            current = (System.currentTimeMillis() - CUSTOM_EPOCH) / 100L;
        } while (current <= lastMillis);
        return current;
    }

    private String encodeId(long id) {
        long maskedId = id & ID_MASK;
        long obfuscated = (maskedId ^ OBFUSCATION_KEY) & ID_MASK;
        String decimal = Long.toUnsignedString(obfuscated, 10);
        if (decimal.length() >= SLUG_LENGTH) {
            return decimal.substring(decimal.length() - SLUG_LENGTH);
        }
        StringBuilder sb = new StringBuilder(SLUG_LENGTH);
        for (int i = 0; i < SLUG_LENGTH - decimal.length(); i++) {
            sb.append('0');
        }
        sb.append(decimal);
        return sb.toString();
    }
}
