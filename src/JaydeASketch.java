import javax.swing.*;

public class JaydeASketch extends JPanel {

    public JaydeASketch() { }

    public void initialize() {

    }

    public static void main(String[] args) { SwingUtilities.invokeLater(JaydeASketch::createAndShowDisplay);}

    public static void createAndShowDisplay() {
        JaydeASketch jaydeASketch = new JaydeASketch();
        jaydeASketch.initialize();
    }
}
