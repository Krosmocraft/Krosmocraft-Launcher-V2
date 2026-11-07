package fr.krosmocraft.bootstrap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.lang.reflect.InvocationTargetException;

final class Splash {

	private final JFrame frame = new JFrame("Krosmocraft");
	private final JProgressBar bar = new JProgressBar(0, 1000);

	private Splash() {
		JPanel root = new JPanel(new BorderLayout(0, 12));
		root.setBackground(new Color(16, 16, 20));
		root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
		Image logo = image("logo.png");
		if (logo != null) {
			root.add(new JLabel(new ImageIcon(logo.getScaledInstance(-1, 160, Image.SCALE_SMOOTH))), BorderLayout.CENTER);
		}
		bar.setIndeterminate(true);
		root.add(bar, BorderLayout.SOUTH);
		frame.setUndecorated(true);
		frame.setContentPane(root);
		frame.setSize(420, 260);
		frame.setLocationRelativeTo(null);
		Image icon = image("favicon.png");
		if (icon != null) {
			frame.setIconImage(icon);
		}
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	static Splash show() throws InterruptedException, InvocationTargetException {
		Splash[] s = new Splash[1];
		SwingUtilities.invokeAndWait(() -> {
			s[0] = new Splash();
			s[0].frame.setVisible(true);
		});
		return s[0];
	}

	void progress(double fraction) {
		SwingUtilities.invokeLater(() -> {
			bar.setIndeterminate(false);
			bar.setValue((int) (fraction * 1000));
		});
	}

	void fail(String message) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(frame, message, "Krosmocraft", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		});
	}

	void close() {
		frame.dispose();
	}

	private static Image image(String name) {
		try (var in = Splash.class.getResourceAsStream(name)) {
			return in != null ? ImageIO.read(in) : null;
		} catch (java.io.IOException e) {
			return null;
		}
	}
}
