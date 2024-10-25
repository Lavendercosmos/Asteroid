package se233.asteroid.View;
import javax.swing.*;
import java.awt.*;
import javax.swing.*;
import java.awt.*;

public class GameStage extends JPanel {

        private ImageIcon gifIcon;

        public GameStage() {
            // โหลดไฟล์ GIF
            gifIcon = new ImageIcon("src/main/resources/se233/asteroid/assets/Backgrounds/SpaceBG.gif"); // เปลี่ยนชื่อไฟล์ตามที่คุณใช้
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // วาด GIF
            g.drawImage(gifIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
        }

        public static void main(String[] args) {
            JFrame frame = new JFrame("Game");
            GameStage panel = new GameStage();
            frame.add(panel);
            frame.setSize(800, 600); // ขนาดหน้าต่าง
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }
    }


