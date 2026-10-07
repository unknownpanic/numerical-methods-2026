package util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Мінімальний клас для побудови графіків (без зовнішніх бібліотек) через Java2D. */
public class Chart {
    static class Series {
        String name; double[] x, y; Color color; boolean line, points, dashed;
    }
    final String title, xLabel, yLabel;
    final List<Series> series = new ArrayList<>();
    Double yMin, yMax, xMin, xMax;
    int width = 1000, height = 600;

    public static final Color[] PALETTE = {
        new Color(0x1f77b4), new Color(0xd62728), new Color(0x2ca02c), new Color(0xff7f0e),
        new Color(0x9467bd), new Color(0x8c564b), new Color(0x17becf), Color.BLACK };

    public Chart(String title, String xLabel, String yLabel) {
        this.title = title; this.xLabel = xLabel; this.yLabel = yLabel;
    }
    public Chart ylim(double lo, double hi) { yMin = lo; yMax = hi; return this; }
    public Chart xlim(double lo, double hi) { xMin = lo; xMax = hi; return this; }

    public Chart line(String name, double[] x, double[] y, Color c, boolean dashed) {
        Series s = new Series(); s.name = name; s.x = x; s.y = y; s.color = c; s.line = true; s.dashed = dashed;
        series.add(s); return this;
    }
    public Chart points(String name, double[] x, double[] y, Color c) {
        Series s = new Series(); s.name = name; s.x = x; s.y = y; s.color = c; s.points = true;
        series.add(s); return this;
    }

    static double niceStep(double range, int target) {
        double raw = range / target, mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double f = raw / mag;
        double nf = f < 1.5 ? 1 : f < 3 ? 2 : f < 7 ? 5 : 10;
        return nf * mag;
    }
    static String fmt(double v, double step) {
        if (Math.abs(v) >= 1e6 || (Math.abs(v) < 1e-3 && v != 0)) return String.format("%.1e", v);
        if (step >= 1) return String.format("%.0f", v);
        int d = (int) Math.max(0, Math.ceil(-Math.log10(step)));
        return String.format("%." + d + "f", v);
    }

    public void save(String path) throws Exception {
        System.setProperty("java.awt.headless", "true");
        double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y0 = Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (Series s : series) for (int i = 0; i < s.x.length; i++) {
            if (Double.isNaN(s.y[i]) || Double.isInfinite(s.y[i])) continue;
            x0 = Math.min(x0, s.x[i]); x1 = Math.max(x1, s.x[i]);
            y0 = Math.min(y0, s.y[i]); y1 = Math.max(y1, s.y[i]);
        }
        if (xMin != null) x0 = xMin;
        if (xMax != null) x1 = xMax;
        if (yMin == null && yMax == null) { double p = (y1 - y0) * 0.05; if (p == 0) p = 1; y0 -= p; y1 += p; }
        if (yMin != null) y0 = yMin;
        if (yMax != null) y1 = yMax;
        double xs = niceStep(x1 - x0, 8), ys = niceStep(y1 - y0, 8);

        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
        Font base = new Font("SansSerif", Font.PLAIN, 14);
        g.setFont(base);
        int lw = 0;
        for (Series s : series) lw = Math.max(lw, g.getFontMetrics().stringWidth(s.name));
        int L = 90, R = lw + 85, T = 50, B = 65;
        int pw = width - L - R, ph = height - T - B;
        g.setStroke(new BasicStroke(1f));
        for (double v = Math.ceil(x0 / xs) * xs; v <= x1 + 1e-9; v += xs) {
            int px = L + (int) ((v - x0) / (x1 - x0) * pw);
            g.setColor(new Color(225, 225, 225)); g.drawLine(px, T, px, T + ph);
            g.setColor(Color.BLACK); String s = fmt(v, xs);
            g.drawString(s, px - g.getFontMetrics().stringWidth(s) / 2, T + ph + 20);
        }
        for (double v = Math.ceil(y0 / ys) * ys; v <= y1 + 1e-9; v += ys) {
            int py = T + ph - (int) ((v - y0) / (y1 - y0) * ph);
            g.setColor(new Color(225, 225, 225)); g.drawLine(L, py, L + pw, py);
            g.setColor(Color.BLACK); String s = fmt(v, ys);
            g.drawString(s, L - 8 - g.getFontMetrics().stringWidth(s), py + 5);
        }
        g.setColor(Color.BLACK); g.drawRect(L, T, pw, ph);
        g.setFont(base.deriveFont(Font.BOLD, 17f));
        g.drawString(title, L + (pw - g.getFontMetrics().stringWidth(title)) / 2, 30);
        g.setFont(base.deriveFont(15f));
        g.drawString(xLabel, L + (pw - g.getFontMetrics().stringWidth(xLabel)) / 2, height - 15);
        AffineTransform old = g.getTransform();
        g.rotate(-Math.PI / 2);
        g.drawString(yLabel, -(T + ph / 2 + g.getFontMetrics().stringWidth(yLabel) / 2), 22);
        g.setTransform(old);
        g.setClip(L, T, pw, ph);
        for (Series s : series) {
            g.setColor(s.color);
            if (s.line) {
                g.setStroke(s.dashed ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{8f, 6f}, 0f)
                                      : new BasicStroke(2f));
                Path2D p = new Path2D.Double(); boolean start = true;
                for (int i = 0; i < s.x.length; i++) {
                    double px = L + (s.x[i] - x0) / (x1 - x0) * pw, py = T + ph - (s.y[i] - y0) / (y1 - y0) * ph;
                    if (start) { p.moveTo(px, py); start = false; } else p.lineTo(px, py);
                }
                g.draw(p);
            }
            if (s.points) {
                for (int i = 0; i < s.x.length; i++) {
                    double px = L + (s.x[i] - x0) / (x1 - x0) * pw, py = T + ph - (s.y[i] - y0) / (y1 - y0) * ph;
                    g.fill(new Ellipse2D.Double(px - 5, py - 5, 10, 10));
                }
            }
        }
        g.setClip(null);
        g.setFont(base);
        int lx = L + pw + 16, ly = T + 4, lh = series.size() * 20 + 8;
        g.setColor(new Color(255, 255, 255, 230)); g.fillRect(lx - 6, ly - 4, lw + 50, lh);
        g.setColor(Color.GRAY); g.setStroke(new BasicStroke(1f)); g.drawRect(lx - 6, ly - 4, lw + 50, lh);
        int k = 0;
        for (Series s : series) {
            int yy = ly + 14 + k * 20; g.setColor(s.color);
            if (s.line) {
                g.setStroke(s.dashed ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{6f, 4f}, 0f) : new BasicStroke(2f));
                g.drawLine(lx, yy - 4, lx + 30, yy - 4);
            }
            if (s.points) g.fill(new Ellipse2D.Double(lx + 10, yy - 9, 10, 10));
            g.setColor(Color.BLACK); g.drawString(s.name, lx + 38, yy);
            k++;
        }
        g.dispose();
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
    }
}
