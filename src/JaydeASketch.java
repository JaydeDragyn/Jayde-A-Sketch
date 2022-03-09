import javax.swing.*;
import java.awt.*;

public class JaydeASketch extends JPanel {

    private JFrame frame;
    private final Dimension PANEL_SIZE;

    public JaydeASketch() {
        PANEL_SIZE = new Dimension(800,600);
    }

    public void initialize() {
        initPanel();
        initFrame();

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

    public static void main(String[] args) { SwingUtilities.invokeLater(JaydeASketch::createAndShowDisplay);}

    public static void createAndShowDisplay() {
        JaydeASketch jaydeASketch = new JaydeASketch();
        jaydeASketch.initialize();
    }
}
