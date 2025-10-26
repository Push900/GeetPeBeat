import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Hashtable;

public class MusicPlayerGUI extends JFrame {
    
    public static final Color FRAME_COLOR = Color.BLACK;
    public static final Color TEXT_COLOR = Color.WHITE;

    private MusicPlayer musicPlayer;

    
    private JFileChooser jFileChooser;

    private JLabel songTitle, songArtist;
    private JPanel playbackBtns;
    private JSlider playbackSlider;

    public MusicPlayerGUI(){
        
        super("Music Player");

        setSize(400, 600);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        // prevent the app from being resized
        setResizable(false);

        setLayout(null);
        getContentPane().setBackground(FRAME_COLOR);

        musicPlayer = new MusicPlayer(this);
        jFileChooser = new JFileChooser();

        // set a default path for file explorer
        File defaultDir = new File("src/assets");
        if(!defaultDir.exists()) {
            defaultDir = new File("assets");
        }
        if(!defaultDir.exists()) {
            defaultDir = new File(".");
        }
        jFileChooser.setCurrentDirectory(defaultDir);

        // filter file chooser to only see .mp3 files
        jFileChooser.setFileFilter(new FileNameExtensionFilter("MP3", "mp3"));

        addGuiComponents();
    }

    private void addGuiComponents(){
        // add toolbar
        addToolbar();

    // load record image and scale it to fit while preserving aspect ratio
    int imgW = getWidth() - 20;
    int imgH = 225;
    JLabel songImage = new JLabel(loadAndScaleImage("src/assets/record.png", imgW, imgH));
    songImage.setBounds(0, 50, imgW, imgH);
    songImage.setHorizontalAlignment(SwingConstants.CENTER);
        add(songImage);

        // song title
        songTitle = new JLabel("Song Title");
        songTitle.setBounds(0, 285, getWidth() - 10, 30);
        songTitle.setFont(new Font("Dialog", Font.BOLD, 24));
        songTitle.setForeground(TEXT_COLOR);
        songTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(songTitle);

        // song artist
        songArtist = new JLabel("Artist");
        songArtist.setBounds(0, 315, getWidth() - 10, 30);
        songArtist.setFont(new Font("Dialog", Font.PLAIN, 24));
        songArtist.setForeground(TEXT_COLOR);
        songArtist.setHorizontalAlignment(SwingConstants.CENTER);
        add(songArtist);

        // playback slider
        playbackSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        playbackSlider.setBounds(getWidth()/2 - 300/2, 365, 300, 40);
        playbackSlider.setBackground(null);
        playbackSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // when the user is holding the tick we want to the pause the song
                musicPlayer.pauseSong();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // when the user drops the tick
                // guard: no-op if no song is loaded to avoid NPE
                if(musicPlayer.getCurrentSong() == null) return;

                JSlider source = (JSlider) e.getSource();

                // get the frame value from where the user wants to playback to
                int frame = source.getValue();

                // update the current frame in the music player to this frame
                musicPlayer.setCurrentFrame(frame);

                // update current time in milli as well
                musicPlayer.setCurrentTimeInMilli((int) (frame / (2.08 * musicPlayer.getCurrentSong().getFrameRatePerMilliseconds())));

                // resume the song
                musicPlayer.playCurrentSong();

                // toggle on pause button and toggle off play button
                enablePauseButtonDisablePlayButton();
            }
        });
        add(playbackSlider);

        // playback buttons (i.e. previous, play, next)
        addPlaybackBtns();
    }

    private void addToolbar(){
        JToolBar toolBar = new JToolBar();
        toolBar.setBounds(0, 0, getWidth(), 20);

        // prevent toolbar from being moved
        toolBar.setFloatable(false);

        // add drop down menu
        JMenuBar menuBar = new JMenuBar();
        toolBar.add(menuBar);

        // now we will add a song menu where we will place the loading song option
        JMenu songMenu = new JMenu("Song");
        menuBar.add(songMenu);

        // add the "load song" item in the songMenu
        JMenuItem loadSong = new JMenuItem("Load Song");
        loadSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // an integer is returned to us to let us know what the user did
                int result = jFileChooser.showOpenDialog(MusicPlayerGUI.this);
                File selectedFile = jFileChooser.getSelectedFile();

                // this means that we are also checking to see if the user pressed the "open" button
                if(result == JFileChooser.APPROVE_OPTION && selectedFile != null){
                    // create a song obj based on selected file
                    Song song = new Song(selectedFile.getPath());

                    // load song in music player
                    musicPlayer.loadSong(song);

                    // update song title and artist
                    updateSongTitleAndArtist(song);

                    // update playback slider
                    updatePlaybackSlider(song);

                    // toggle on pause button and toggle off play button
                    enablePauseButtonDisablePlayButton();
                }
            }
        });
        songMenu.add(loadSong);

        // now we will add the playlist menu
        JMenu playlistMenu = new JMenu("Playlist");
        menuBar.add(playlistMenu);

        // then add the items to the playlist menu
        JMenuItem createPlaylist = new JMenuItem("Create Playlist");
        createPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // load music playlist dialog
                new MusicPlaylistDialog(MusicPlayerGUI.this).setVisible(true);
            }
        });
        playlistMenu.add(createPlaylist);

        JMenuItem loadPlaylist = new JMenuItem("Load Playlist");
        loadPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileFilter(new FileNameExtensionFilter("Playlist", "txt"));
                jFileChooser.setCurrentDirectory(new File("src/assets"));

                int result = jFileChooser.showOpenDialog(MusicPlayerGUI.this);
                File selectedFile = jFileChooser.getSelectedFile();

                if(result == JFileChooser.APPROVE_OPTION && selectedFile != null){
                    // stop the music
                    musicPlayer.stopSong();

                    // load playlist
                    musicPlayer.loadPlaylist(selectedFile);
                }
            }
        });
        playlistMenu.add(loadPlaylist);

        add(toolBar);
    }

    private void addPlaybackBtns(){
        playbackBtns = new JPanel();
        playbackBtns.setBounds(0, 435, getWidth() - 10, 80);
        playbackBtns.setBackground(null);

        // previous button
    JButton prevButton = new JButton(loadImage("/assets/previous.png"));
        prevButton.setBorderPainted(false);
        prevButton.setBackground(null);
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // go to the previous song
                musicPlayer.prevSong();
            }
        });
        playbackBtns.add(prevButton);

        // play button
    JButton playButton = new JButton(loadImage("/assets/play.png"));
        playButton.setBorderPainted(false);
        playButton.setBackground(null);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // toggle off play button and toggle on pause button
                enablePauseButtonDisablePlayButton();

                // play or resume song
                musicPlayer.playCurrentSong();
            }
        });
        playbackBtns.add(playButton);

        // pause button
    JButton pauseButton = new JButton(loadImage("/assets/pause.png"));
        pauseButton.setBorderPainted(false);
        pauseButton.setBackground(null);
        pauseButton.setVisible(false);
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // toggle off pause button and toggle on play button
                enablePlayButtonDisablePauseButton();

                // pause the song
                musicPlayer.pauseSong();
            }
        });
        playbackBtns.add(pauseButton);

        // next button
    JButton nextButton = new JButton(loadImage("/assets/next.png"));
        nextButton.setBorderPainted(false);
        nextButton.setBackground(null);
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // go to the next song
                musicPlayer.nextSong();
            }
        });
        playbackBtns.add(nextButton);

        add(playbackBtns);
    }

    // this will be used to update our slider from the music player class
    public void setPlaybackSliderValue(int frame){
        playbackSlider.setValue(frame);
    }

    public void updateSongTitleAndArtist(Song song){
        songTitle.setText(song.getSongTitle());
        songArtist.setText(song.getSongArtist());
    }

    public void updatePlaybackSlider(Song song){
        // update max count for slider
        playbackSlider.setMaximum(song.getMp3File().getFrameCount());

        // create the song length label
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        // beginning will be 00:00
        JLabel labelBeginning = new JLabel("00:00");
        labelBeginning.setFont(new Font("Dialog", Font.BOLD, 18));
        labelBeginning.setForeground(TEXT_COLOR);

        // end will vary depending on the song
        JLabel labelEnd =  new JLabel(song.getSongLength());
        labelEnd.setFont(new Font("Dialog", Font.BOLD, 18));
        labelEnd.setForeground(TEXT_COLOR);

        labelTable.put(0, labelBeginning);
        labelTable.put(song.getMp3File().getFrameCount(), labelEnd);

        playbackSlider.setLabelTable(labelTable);
        playbackSlider.setPaintLabels(true);
    }

    public void enablePauseButtonDisablePlayButton(){
        // retrieve reference to play button from playbackBtns panel
        JButton playButton = (JButton) playbackBtns.getComponent(1);
        JButton pauseButton = (JButton) playbackBtns.getComponent(2);

        // turn off play button
        playButton.setVisible(false);
        playButton.setEnabled(false);

        // turn on pause button
        pauseButton.setVisible(true);
        pauseButton.setEnabled(true);
    }

    public void enablePlayButtonDisablePauseButton(){
        // retrieve reference to play button from playbackBtns panel
        JButton playButton = (JButton) playbackBtns.getComponent(1);
        JButton pauseButton = (JButton) playbackBtns.getComponent(2);

        // turn on play button
        playButton.setVisible(true);
        playButton.setEnabled(true);

        // turn off pause button
        pauseButton.setVisible(false);
        pauseButton.setEnabled(false);
    }

    private ImageIcon loadImage(String imagePath){
        try{
            // try loading from file first
            File f = new File(imagePath);
            BufferedImage image = null;
            if(f.exists()){
                image = ImageIO.read(f);
            } else {
                // if file not found, try as a classpath resource
                String resourcePath = imagePath;
                if(resourcePath.startsWith("src/")) resourcePath = resourcePath.substring(4);
                if(!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;
                try (InputStream is = MusicPlayerGUI.class.getResourceAsStream(resourcePath)){
                    if(is != null) image = ImageIO.read(is);
                }catch(Exception ignored){ }
            }

            if(image != null) return new ImageIcon(image);
        }catch(Exception e){
            e.printStackTrace();
        }

        // could not find resource
        return null;
    }

    /**
     * Load an image (from file or classpath) and return a scaled ImageIcon that fits within
     * the given width/height while preserving aspect ratio.
     */
    private ImageIcon loadAndScaleImage(String imagePath, int maxWidth, int maxHeight) {
        try {
            BufferedImage image = null;

            // try file first
            File f = new File(imagePath);
            if (f.exists()) {
                image = ImageIO.read(f);
            } else {
                String resourcePath = imagePath;
                if (resourcePath.startsWith("src/")) resourcePath = resourcePath.substring(4);
                if (!resourcePath.startsWith("/")) resourcePath = "/" + resourcePath;
                try (InputStream is = MusicPlayerGUI.class.getResourceAsStream(resourcePath)) {
                    if (is != null) image = ImageIO.read(is);
                } catch (Exception ignored) { }
            }

            if (image == null) return null;

            int iw = image.getWidth();
            int ih = image.getHeight();

            double scale = Math.min((double) maxWidth / iw, (double) maxHeight / ih);
            if (scale <= 0) scale = 1.0;

            int nw = (int) Math.round(iw * scale);
            int nh = (int) Math.round(ih * scale);

            Image scaled = image.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}









