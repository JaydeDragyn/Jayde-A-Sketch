import javax.swing.*;
import java.awt.*;

public class JaydeASketch extends JPanel {

    private JFrame frame;

    public JaydeASketch() { }

    public void initialize() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);

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
