package com.irp.incident.knowledge;

import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Deterministic 384-dim embedder so RAG works without an external model API.
 * Token hashes land in fixed buckets; overlapping terms yield higher cosine
 * similarity. Swap this for a real embedding model later without changing
 * the retrieve API.
 */
@Component
public class HashingEmbedder {

    public static final int DIMENSIONS = 384;

    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        int count = 0;
        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[bucket] += 1.0f;
            count++;
        }
        if (count == 0) {
            return vector;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return vector;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
        return vector;
    }

    public static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    public static String toPgVector(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 8);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
