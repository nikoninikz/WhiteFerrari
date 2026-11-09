package gameDev;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.sound.sampled.*;


public class MainMenu extends JPanel implements KeyListener {

    static final int LW = RoninVillage.LW, LH = RoninVillage.LH;

    //editable ni
    static final String TITLE = "CURSED RONIN";
    static final String TAGLINE = "A RONIN WANDERS THE ASHES";
    static final String[] MAIN_ITEMS = {"PLAY", "CONTROLS", "QUIT"};
    static final String[] CONTROLS = {"WASD / ARROWS: MOVE", "SHIFT: RUN", "ESC: QUIT GAME"};

    //layout
    static final int MENU_X = 74, MAIN_Y = 130, CTRL_BACK_Y = 204, MENU_STEP = 28, MENU_SC = 3;
    static final int GX = 392;   // x sa torii gate / moon
    static final Color CREAM = RoninVillage.col(0xf0d9b8), DIM = RoninVillage.col(0x9a8576),
            GOLD = RoninVillage.col(0xf2c14e), EDGE = RoninVillage.col(0x6a4a54);
    //menu state
    final BufferedImage buf = new BufferedImage(LW, LH, BufferedImage.TYPE_INT_RGB);
    final javax.swing.Timer timer;
    Clip bgMusic;
    Clip loadingSound;
    Clip gameMusic;
    int screen = 0; 
    int sel = 0;
    double time = 0, fadeIn = 1, leaving = -1;
    long last = System.nanoTime();
    double scale = 1; int offX, offY;
    boolean launched = false;
    boolean loading = false;
    double loadingTime = 0;

    MainMenu() {
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = hitTest(e);
                if (i >= 0) sel = i;
                setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
            @Override public void mouseClicked(MouseEvent e) {
                int i = hitTest(e);
                if (i >= 0) {
                    sel = i;
                    playButtonSound();
                    confirm();
                }
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        timer = new javax.swing.Timer(16, e -> { tick(); repaint(); });
        timer.start();
        
        playMusic();
    }

    static String[] items(int screen) { return screen == 0 ? MAIN_ITEMS : new String[]{"BACK"}; }

    static Rectangle itemRect(int screen, int i) {
        String s = items(screen)[i];
        int y = (screen == 0 ? MAIN_Y : CTRL_BACK_Y) + i * MENU_STEP;
        return new Rectangle(MENU_X - 24, y - 4, RoninVillage.textW(s, MENU_SC) + 40, 5 * MENU_SC + 8);
    }

    int hitTest(MouseEvent e) {
        int lx = (int) ((e.getX() - offX) / scale), ly = (int) ((e.getY() - offY) / scale);
        int n = items(screen).length;
        for (int i = 0; i < n; i++) if (itemRect(screen, i).contains(lx, ly)) return i;
        return -1;
    }

    void confirm() {
        if (leaving >= 0) return;
        if (screen == 1) { screen = 0; sel = 1; return; }
        switch (sel) {
            case 0: leaving = 0; break;                 // PLAY: fade out, then start the game
            case 1: screen = 1; sel = 0; break;         // CONTROLS
            default: System.exit(0);                    // QUIT
        }
    }

    void tick() {
        long now = System.nanoTime();
        double dt = Math.min(.05, (now - last) / 1e9); last = now;
        time += dt;
        if (fadeIn > 0) fadeIn = Math.max(0, fadeIn - dt / .8);
        if (leaving >= 0) 
            if (!loading) {
                leaving += dt / .5;
                if (leaving >= 1) {
                    leaving = 1;
                    loading = true;
                    playLoadingSound();

                    if (bgMusic != null && bgMusic.isRunning()) {
                        bgMusic.stop();
                    }
                    playLoadingSound();
                }
            }
            else {
                loadingTime += dt;
                if (loadingTime >= 2.5) {
                    launch();
                }
            }
        }
    

    void launch() {
        if (launched) return;
        launched = true;
        timer.stop();
        if (bgMusic != null) {
            bgMusic.stop();
            bgMusic.close();
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        if (!(w instanceof JFrame)) return;
        JFrame f = (JFrame) w;
        RoninVillage game = new RoninVillage();
        game.setPreferredSize(getPreferredSize());
        f.setContentPane(game);
        f.revalidate(); f.repaint();
        SwingUtilities.invokeLater(game::requestFocusInWindow);
    }

    @Override public void keyPressed(KeyEvent e) {

        if (leaving >= 0) return;
        int k = e.getKeyCode();
        int n = items(screen).length;
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
            sel = (sel + n - 1) % n;
            playButtonSound();
        } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
            sel = (sel + 1) % n;
            playButtonSound();
        } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            playButtonSound();
            confirm();
        } else if (k == KeyEvent.VK_ESCAPE) {
            if (screen == 1) {
                playButtonSound();
                screen = 0;
                sel = 1;
            } else {
                System.exit(0);
            }
        }
    }
    @Override public void keyReleased(KeyEvent e) { }
    @Override public void keyTyped(KeyEvent e) { }

    static double wx(int px, int py) { return (py / 8.0 + px / 16.0) / 2; }
    static double wy(int px, int py) { return (py / 8.0 - px / 16.0) / 2; }

    static void moon(Graphics2D g, int cx, int cy, int r) {
        for (int k = 7; k >= 1; k--) {
            int rr = r + k * 6;
            g.setColor(new Color(255, 214, 190, 7)); g.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }
        g.setColor(RoninVillage.col(0xe9d7c0)); g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(RoninVillage.col(0xd2bba5));
        int[][] cr = {{-12, -8, 7}, {10, -14, 5}, {6, 10, 9}, {-16, 12, 4}, {16, 2, 4}};
        for (int[] c : cr) g.fillOval(cx + c[0] - c[2], cy + c[1] - c[2], c[2] * 2, c[2] * 2);
    }

    static void ridge(Graphics2D g, Color c, int base, double amp, int seed, double freq) {
        int n = LW / 6 + 2;
        int[] xs = new int[n + 2], ys = new int[n + 2];
        for (int i = 0; i < n; i++) {
            int x = i * 6;
            double h = Math.sin(x * freq + seed) * .5 + Math.sin(x * freq * 2.3 + seed * 1.7) * .3 + RoninVillage.hash(i, 0, seed) * .35;
            xs[i] = x; ys[i] = (int) (base - amp * (h + .5));
        }
        xs[n] = LW + 6; ys[n] = LH; xs[n + 1] = 0; ys[n + 1] = LH;
        g.setColor(c); g.fillPolygon(xs, ys, n + 2);
    }

    static void ground(Graphics2D g) {
        ridge(g, RoninVillage.col(0x1b121a), 228, 5, 9, .02);
        for (int y = 230; y < LH; y += 4) { g.setColor(new Color(0, 0, 0, 12 + (y - 230) / 2)); g.fillRect(0, y, LW, 4); }
        for (int i = 0; i < 170; i++) {
            int x = (int) (RoninVillage.hash(i, 1, 4) * LW), y = 232 + (int) (RoninVillage.hash(i, 2, 4) * (LH - 234));
            g.setColor(RoninVillage.hash(i, 3, 4) > .5 ? RoninVillage.col(0x2e2229) : RoninVillage.col(0x0d080d));
            g.fillRect(x, y, 2, 1);
        }
        for (int i = 0; i < 30; i++) { // sickly grass tufts, same look as the game
            int x = (int) (RoninVillage.hash(i, 5, 8) * LW), y = 236 + (int) (RoninVillage.hash(i, 6, 8) * (LH - 244));
            g.setColor(RoninVillage.col(0x4a4a28));
            g.fillRect(x, y - 3, 1, 3); g.fillRect(x - 2, y - 2, 1, 2); g.fillRect(x + 2, y - 2, 1, 2);
        }
    }

    static void gate(Graphics2D g, int gx, int base) {
        Color post = RoninVillage.col(0x5a1d20), hi = RoninVillage.col(0x9a3330), dk = RoninVillage.col(0x2e0f12),
                blk = RoninVillage.col(0x1d1418), red = RoninVillage.col(0x7a2428);
        int half = 44, pw = 9;
        for (int s = -1; s <= 1; s += 2) {
            int px = gx + s * half - pw / 2;
            RoninVillage.shadowOval(px + pw / 2, base, 11, 3);
            g.setColor(post); g.fillRect(px, 104, pw, base - 104);
            g.setColor(hi); g.fillRect(px, 104, 2, base - 104);                 //moonlit edge
            g.setColor(dk); g.fillRect(px + pw - 2, 104, 2, base - 104);
            g.setColor(blk); g.fillRect(px - 1, base - 6, pw + 2, 6);           //stone foot
        }
        g.setColor(red); g.fillRect(gx - half, 128, half * 2, 6);               //lower tie beam
        g.setColor(dk); g.fillRect(gx - half, 133, half * 2, 1);
        g.setColor(blk); g.fillRect(gx - 4, 113, 8, 15);                        //centre plaque
        g.setColor(red); g.fillRect(gx - half - 8, 106, half * 2 + 16, 6);      //upper tie beam
        g.setColor(dk); g.fillRect(gx - half - 8, 111, half * 2 + 16, 1);
        int[] xs = {gx - 66, gx - 40, gx + 40, gx + 66, gx + 61, gx + 40, gx - 40, gx - 61};
        int[] ys = {90, 97, 97, 90, 99, 106, 106, 99};
        g.setColor(blk); g.fillPolygon(xs, ys, xs.length);                      //curved top beam
        g.setColor(RoninVillage.col(0x4a363c));
        g.drawLine(gx - 66, 90, gx - 40, 97); g.drawLine(gx - 40, 97, gx + 40, 97); g.drawLine(gx + 40, 97, gx + 66, 90);
    }

    static void part(Graphics2D g, BufferedImage s, int feetX, int top, boolean faceRight, int r0, int r1, int dy) {
        int ax = RoninVillage.SPR_AX, w = s.getWidth();
        if (faceRight) g.drawImage(s, feetX - ax, top + r0 + dy, feetX - ax + w, top + r1 + dy, 0, r0, w, r1, null);
        else g.drawImage(s, feetX + ax + 1, top + r0 + dy, feetX + ax + 1 - w, top + r1 + dy, 0, r0, w, r1, null);
    }

    static void ronin(Graphics2D g, int feetX, int feetY, boolean faceRight, double time) {
        BufferedImage s = RoninVillage.SPRITE;
        int h = s.getHeight(), leg = RoninVillage.SPR_LEG, top = feetY - h + 2;
        int bob = Math.sin(time * 2.0) > .6 ? -1 : 0;
        RoninVillage.shadowOval(feetX, feetY - 1, 11, 3);
        part(g, s, feetX, top, faceRight, leg, h, 0);
        part(g, s, feetX, top, faceRight, 0, leg + 1, bob);
    }

    static void title(Graphics2D g, double time) {

        int sc = 5, w = RoninVillage.textW(TITLE, sc);
        int x = (LW - w) / 2, y = 28;

        RoninVillage.text(TITLE, x + 2, y + 3, sc, new Color(0, 0, 0, 170));
        RoninVillage.text(TITLE, x + 1, y + 1, sc,
                RoninVillage.mix(RoninVillage.col(0x8a1f24),
                RoninVillage.col(0xd8481f), .5 + .5 * Math.sin(time * 2)));
        RoninVillage.text(TITLE, x, y, sc, RoninVillage.col(0xf5e1be));
        String kanji = "呪われた浪人";
        g.setFont(new Font("Dialog", Font.BOLD, 18));
        g.setColor(RoninVillage.col(0xf5e1be));
        FontMetrics fm = g.getFontMetrics();
        int kanjiX = (LW - fm.stringWidth(kanji)) / 2;
        g.drawString(kanji, kanjiX, y + 49);
        g.setColor(EDGE);
        g.fillRect(x, y + 57, w, 1);
        int w2 = RoninVillage.textW(TAGLINE, 2);
        RoninVillage.textS(TAGLINE, (LW - w2) / 2, y + 64, 2,
                new Color(205, 180, 160, 230));
    }

    static void items(Graphics2D g, int screen, int sel, double time) {
        String[] it = items(screen);
        for (int i = 0; i < it.length; i++) {
            Rectangle r = itemRect(screen, i);
            int y = r.y + 4;
            boolean on = i == sel;
            if (on) {
                g.setColor(new Color(122, 30, 34, 120)); g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(RoninVillage.col(0xa83530)); g.fillRect(r.x, r.y, 2, r.height);
                int bob = (int) Math.round(Math.sin(time * 6) * 2);
                RoninVillage.textS(">", MENU_X - 16 + bob, y, MENU_SC, GOLD);
            }
            Color c = on ? RoninVillage.mix(CREAM, GOLD, .5 + .5 * Math.sin(time * 5)) : DIM;
            RoninVillage.textS(it[i], MENU_X, y, MENU_SC, c);
        }
    }

    static void controls(Graphics2D g, double time) {
        int px = MENU_X - 24, py = 96, pw = 236, ph = 138;
        g.setColor(new Color(10, 6, 14, 170)); g.fillRect(px, py, pw, ph);
        g.setColor(EDGE); g.drawRect(px, py, pw - 1, ph - 1);
        RoninVillage.textS("CONTROLS", MENU_X, 108, 3, GOLD);
        g.setColor(EDGE); g.fillRect(MENU_X, 128, RoninVillage.textW("CONTROLS", 3), 1);
        for (int i = 0; i < CONTROLS.length; i++) RoninVillage.textS(CONTROLS[i], MENU_X, 140 + i * 20, 2, CREAM);
        items(g, 1, 0, time);
    }

    static void renderMenu(BufferedImage out, int screen, int sel, double time, double fade) {
        Graphics2D g = out.createGraphics();
        RoninVillage.g = g; RoninVillage.time = time; RoninVillage.camX = 0; RoninVillage.camY = 0;
        RoninVillage.lights.clear();
        RoninVillage.drawBackground();
        moon(g, GX, 92, 34);
        ridge(g, RoninVillage.col(0x2c2238), 172, 38, 3, .021);
        ridge(g, RoninVillage.col(0x1a1223), 196, 30, 8, .027);
        ground(g);
        RoninVillage.drawTree(wx(292, 248), wy(292, 248), 78, 5);
        RoninVillage.drawTree(wx(16, 262), wy(16, 262), 132, 11);
        RoninVillage.drawTree(wx(466, 258), wy(466, 258), 120, 17);
        gate(g, GX, 234);
        RoninVillage.drawFire(wx(372, 240), wy(372, 240), false);
        RoninVillage.lights.add(new int[]{GX, 92, 80, 0x9fb4ff});
        RoninVillage.drawFog();
        RoninVillage.lightPass();
        RoninVillage.drawEmbers();
        RoninVillage.drawVignette();       
        title(g, time);
        if (screen == 0) items(g, 0, sel, time); else controls(g, time);
        String hint = screen == 0 ? "W/S OR ARROWS: SELECT   ENTER: CONFIRM" : "ENTER OR ESC: BACK";
        RoninVillage.textS(hint, (LW - RoninVillage.textW(hint, 1)) / 2, LH - 12, 1, new Color(230, 210, 190, 200));
        if (fade > 0) { g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, fade)))); g.fillRect(0, 0, LW, LH); }
        g.dispose();
    }

    @Override protected void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        double fade = leaving >= 0 ? Math.min(1, leaving) : fadeIn;
        renderMenu(buf, screen, sel, time, fade);
        if (loading) {
            renderLoading(buf, Math.min(1, loadingTime / 1.5), time);
        }
        Graphics2D g2 = (Graphics2D) gr;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        scale = Math.min(getWidth() / (double) LW, getHeight() / (double) LH);
        int w = (int) (LW * scale), h = (int) (LH * scale);
        offX = (getWidth() - w) / 2; offY = (getHeight() - h) / 2;
        g2.drawImage(buf, offX, offY, w, h, null);
    }

    static void screenshots(String dir) throws Exception {
        Object[][] shots = {{0, 0, "menu_main.png"}, {1, 0, "menu_controls.png"}};
        for (Object[] s : shots) {
            BufferedImage lo = new BufferedImage(LW, LH, BufferedImage.TYPE_INT_RGB);
            renderMenu(lo, (Integer) s[0], (Integer) s[1], 1.4, 0);
            BufferedImage big = new BufferedImage(LW * 3, LH * 3, BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = big.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            bg.drawImage(lo, 0, 0, LW * 3, LH * 3, null); bg.dispose();
            ImageIO.write(big, "png", new File(dir, (String) s[2]));
            System.out.println("Wrote " + s[2]);
        }
    }

    public static void main(String[] args) throws Exception {
        RoninVillage.setup();   //game starts instantly
        if (args.length > 0 && args[0].equals("--screenshots")) { screenshots(args.length > 1 ? args[1] : "."); return; }
        SwingUtilities.invokeLater(() -> {
            Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
            int sc = (int) Math.max(1, Math.min(4, Math.floor(Math.min(scr.width * .92 / LW, scr.height * .86 / LH))));
            JFrame f = new JFrame("Ronin Village");
            MainMenu m = new MainMenu();
            m.setPreferredSize(new Dimension(LW * sc, LH * sc));
            f.setContentPane(m);
            f.pack(); f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true);
            m.requestFocusInWindow();
        });
    }
    
    void playMusic() {
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(
                    getClass().getResource("/menu_music.wav")
            );

            bgMusic = AudioSystem.getClip();
            bgMusic.open(audio);
            bgMusic.loop(Clip.LOOP_CONTINUOUSLY);

        } catch (Exception e) {
            System.out.println("Could not load background music!");
            e.printStackTrace();
        }
    }
    
    static void renderLoading(BufferedImage out, double progress, double time) {

        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        progress = Math.max(0, Math.min(1, progress));
        RoninVillage.g = g;
        RoninVillage.time = time;
        RoninVillage.camX = 0;
        RoninVillage.camY = 0;
        RoninVillage.drawBackground();
        RoninVillage.drawFog();
        RoninVillage.drawEmbers();
        // Dark atmospheric overlay
        g.setColor(new Color(8, 6, 12, 155));
        g.fillRect(0, 0, LW, LH);
        // Subtle crimson glow
        for (int i = 5; i >= 1; i--) {
            g.setColor(new Color(120, 25, 35, 8));
            g.fillOval(LW / 2 - i * 35, LH / 2 - i * 20,
                    i * 70, i * 40);
        }
        // Loading title
        String msg = "PREPARING THE VILLAGE";
        g.setColor(RoninVillage.col(0xf0d9b8));
        FontMetrics fm = g.getFontMetrics();
        int tx = (LW - fm.stringWidth(msg)) / 2;
        int sc = 2; // Font scale
        RoninVillage.text(msg, tx, LH / 2 - 22, sc,
                RoninVillage.col(0xf0d9b8));
        // Bar dimensions
        int barW = 190;
        int barH = 12;
        int bx = (LW - barW) / 2;
        int by = LH / 2;
        // Outer shadow
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(bx - 5, by - 5, barW + 10, barH + 10);
        // Bar background
        g.setColor(RoninVillage.col(0x21151c));
        g.fillRect(bx, by, barW, barH);
        // Progress fill
        int fillW = (int)(barW * progress);
        g.setColor(RoninVillage.col(0x751f29));
        g.fillRect(bx, by, fillW, barH);
        // Gold highlight
        if (fillW > 0) {
            g.setColor(RoninVillage.col(0xc34a38));
            g.fillRect(bx, by, fillW, 2);
            g.setColor(RoninVillage.col(0xf2c14e));
            g.fillRect(bx, by, fillW, 1);
        }
        // Gold border
        g.setColor(RoninVillage.col(0xb18a50));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(bx - 1, by - 1, barW + 2, barH + 2);
        // Decorative corners
        int d = 5;
        g.fillRect(bx - 4, by - 4, d, 2);
        g.fillRect(bx - 4, by - 4, 2, d);
        g.fillRect(bx + barW - 1, by - 4, d, 2);
        g.fillRect(bx + barW + 2, by - 4, 2, d);
        g.fillRect(bx - 4, by + barH + 2, d, 2);
        g.fillRect(bx - 4, by + barH - 1, 2, d);
        g.fillRect(bx + barW - 1, by + barH + 2, d, 2);
        g.fillRect(bx + barW + 2, by + barH - 1, 2, d);
        String percent = (int)(progress * 100) + "%";
        g.setFont(new Font("Dialog", Font.BOLD, 11));
        g.setColor(RoninVillage.col(0xf0d9b8));
        fm = g.getFontMetrics();
        g.drawString(percent,
                (LW - fm.stringWidth(percent)) / 2,
                by + 30);
        g.setFont(new Font("Dialog", Font.BOLD, 13));
        g.setColor(RoninVillage.col(0x9a3330));
        String kanji = "呪われた浪人";
        fm = g.getFontMetrics();
        g.drawString(kanji,
                (LW - fm.stringWidth(kanji)) / 2,
                by + 53);
        g.dispose();
    }
    
    void playButtonSound() {
        try {
            File soundFile = new File("res/button.wav");

            System.out.println("Sound file found: " + soundFile.exists());

            AudioInputStream audio = AudioSystem.getAudioInputStream(soundFile);

            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            // Increase volume
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(6.0f);

            clip.start();
         
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void playLoadingSound() {
        try {
            File soundFile = new File("res/LoadingSound.wav");
            AudioInputStream audio = AudioSystem.getAudioInputStream(soundFile);
            loadingSound = AudioSystem.getClip();
            loadingSound.open(audio);
            loadingSound.start();

        } catch (Exception e) {
            System.out.println("Could not load loading sound!");
        }
    }
}