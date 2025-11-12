import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class ArcDemo extends JPanel implements MouseListener, MouseMotionListener {
    private BufferedImage image;
    private Point center;
    private int radius;
    private Point currentPoint;
    private boolean drawing = false;
    private Color startColor = Color.RED;
    private Color endColor = Color.BLUE;

    public ArcDemo() {
        image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        clearImage();
        addMouseListener(this);
        addMouseMotionListener(this);

        // Панель управления
        setupControlPanel();
    }

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel();

        JButton clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> clearImage());

        JButton color1Button = new JButton("Цвет начала");
        color1Button.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Выберите начальный цвет", startColor);
            if (newColor != null) startColor = newColor;
        });

        JButton color2Button = new JButton("Цвет конца");
        color2Button.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Выберите конечный цвет", endColor);
            if (newColor != null) endColor = newColor;
        });

        controlPanel.add(clearButton);
        controlPanel.add(color1Button);
        controlPanel.add(color2Button);

        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
    }

    private void clearImage() {
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this);

        // Рисуем временную линию при создании дуги
        if (drawing && center != null && currentPoint != null) {
            g.setColor(Color.GRAY);
            g.drawLine(center.x, center.y, currentPoint.x, currentPoint.y);

            // Показываем радиус
            radius = (int) center.distance(currentPoint);
            g.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (!drawing) {
                // Первый клик - устанавливаем центр
                center = e.getPoint();
                drawing = true;
            } else {
                // Второй клик - устанавливаем радиус и рисуем дугу
                currentPoint = e.getPoint();
                radius = (int) center.distance(currentPoint);
                drawRandomArc();
                drawing = false;
                center = null;
                currentPoint = null;
            }
        }
    }

    private void drawRandomArc() {
        if (center == null || radius <= 0) return;

        // Случайные углы для дуги
        double startAngle = Math.random() * 360;
        double endAngle = startAngle + 30 + Math.random() * 300; // Дуга от 30 до 330 градусов

        ArcRenderer.drawArc(image, center.x, center.y, radius, startAngle, endAngle, startColor, endColor);
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (drawing) {
            currentPoint = e.getPoint();
            repaint();
        }
    }

    // Неиспользуемые методы интерфейсов
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Интерактивный рисовальщик дуг");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(850, 700);
            frame.add(new ArcDemo());
            frame.setVisible(true);
        });
    }
}

class ArcRenderer {

    public static void drawArc(BufferedImage image, int centerX, int centerY, int radius,
                               double startAngle, double endAngle, Color startColor, Color endColor) {

        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(endAngle);

        int minX = Math.max(0, centerX - radius);
        int maxX = Math.min(image.getWidth() - 1, centerX + radius);
        int minY = Math.max(0, centerY - radius);
        int maxY = Math.min(image.getHeight() - 1, centerY + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (isPointOnArc(x, y, centerX, centerY, radius, startRad, endRad)) {
                    Color pixelColor = calculateColor(x, y, centerX, centerY,
                            startAngle, endAngle, startColor, endColor);
                    image.setRGB(x, y, pixelColor.getRGB());
                }
            }
        }
    }

    private static boolean isPointOnArc(int x, int y, int centerX, int centerY, int radius,
                                        double startRad, double endRad) {
        double dx = x - centerX;
        double dy = y - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (Math.abs(distance - radius) > 1.0) {
            return false;
        }

        double pointAngle = Math.atan2(dy, dx);
        pointAngle = normalizeAngle(pointAngle);
        double normStart = normalizeAngle(startRad);
        double normEnd = normalizeAngle(endRad);

        if (normStart <= normEnd) {
            return pointAngle >= normStart && pointAngle <= normEnd;
        } else {
            return pointAngle >= normStart || pointAngle <= normEnd;
        }
    }

    private static double normalizeAngle(double angle) {
        angle %= 2 * Math.PI;
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    private static Color calculateColor(int x, int y, int centerX, int centerY,
                                        double startAngle, double endAngle,
                                        Color startColor, Color endColor) {

        double dx = x - centerX;
        double dy = y - centerY;
        double currentAngle = Math.atan2(dy, dx);

        double normStart = normalizeAngle(Math.toRadians(startAngle));
        double normEnd = normalizeAngle(Math.toRadians(endAngle));
        double normCurrent = normalizeAngle(currentAngle);

        double t = calculateInterpolationParameter(normCurrent, normStart, normEnd);

        return interpolateColor(startColor, endColor, t);
    }

    private static double calculateInterpolationParameter(double currentAngle,
                                                          double startAngle, double endAngle) {
        if (startAngle <= endAngle) {
            double totalAngle = endAngle - startAngle;
            double relativeAngle = currentAngle - startAngle;
            return Math.max(0, Math.min(1, relativeAngle / totalAngle));
        } else {
            double totalAngle = (2 * Math.PI - startAngle) + endAngle;
            double relativeAngle;
            if (currentAngle >= startAngle) {
                relativeAngle = currentAngle - startAngle;
            } else {
                relativeAngle = (2 * Math.PI - startAngle) + currentAngle;
            }
            return Math.max(0, Math.min(1, relativeAngle / totalAngle));
        }
    }

    private static Color interpolateColor(Color start, Color end, double t) {
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * t);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);

        return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)),
                Math.max(0, Math.min(255, a))
        );
    }
}