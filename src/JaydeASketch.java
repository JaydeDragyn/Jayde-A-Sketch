import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

public class JaydeASketch extends JPanel {

    private JFrame frame;
    private final Dimension PANEL_SIZE;

    private final Color SURFACE_DEFAULT;
    private BufferedImage drawingSurface;
    private Graphics drawingSurfacePen;
    private Point penLocation;

    public JaydeASketch() {
        PANEL_SIZE = new Dimension(800,600);
        SURFACE_DEFAULT = new Color(168,168,168);
    }

    public void initialize() {
        initPanel();
        initFrame();
        initDrawingSurface();
    }

    private void initPanel() {
        setPreferredSize(PANEL_SIZE);
        setBackground(Color.BLACK);
    }

    private void initFrame() {
        frame = new JFrame("Jayde-A-Sketch");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(this);
        frame.pack();
        frame.setVisible(true);
    }

    private void initDrawingSurface() {
        drawingSurface = new BufferedImage(PANEL_SIZE.width, PANEL_SIZE.height, BufferedImage.TYPE_INT_RGB);
        drawingSurfacePen = drawingSurface.getGraphics();
        drawingSurfacePen.setColor(SURFACE_DEFAULT);
        drawingSurfacePen.fillRect(0,0, PANEL_SIZE.width, PANEL_SIZE.height);
        penLocation = new Point(PANEL_SIZE.width / 2, PANEL_SIZE.height / 2);

        paintComponent(getGraphics());
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(drawingSurface, 0, 0, null);
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(JaydeASketch::createAndShowDisplay);}

    public static void createAndShowDisplay() {
        JaydeASketch jaydeASketch = new JaydeASketch();
        jaydeASketch.initialize();
    }
}
