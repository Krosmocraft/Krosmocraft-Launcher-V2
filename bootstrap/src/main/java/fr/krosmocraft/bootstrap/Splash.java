package fr.krosmocraft.bootstrap;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.lang.reflect.InvocationTargetException;

final class Splash {

	private static final int W = 795;
	private static final int H = 448;
	private static final int BAR = 10;

	private final JFrame frame = new JFrame("Krosmocraft");
	private final Image logo = image("logo.png");
	private volatile boolean visible;
	private volatile int percent;

	private Splash() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Image icon = image("favicon.png");
		if (icon != null) {
			frame.setIconImage(icon);
		}
		frame.setSize(W, H);
		frame.setLocationRelativeTo(null);
		frame.setUndecorated(true);
		frame.setBackground(new Color(0, 0, 0, 0));
		JComponent panel = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				if (!visible) {
					return;
				}
				if (logo != null) {
					g.drawImage(logo, 0, 0, W, H, this);
				}
				int w = getWidth();
				int y = getHeight() - BAR;
				g.setColor(Color.GRAY);
				g.fillRect(0, y, w, BAR);
				g.setColor(Color.CYAN);
				g.fillRect(0, y, (int) (w * (percent * 0.01d)), BAR);
			}
		};
		frame.setContentPane(panel);
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
		int p = (int) Math.max(0, Math.min(100, fraction * 100));
		if (visible && p == percent) {
			return;
		}
		percent = p;
		visible = true;
		frame.repaint();
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
