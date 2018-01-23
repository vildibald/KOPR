package Transferer.Gui;

import Transferer.App.FileReceiver;
import Transferer.App.FileSender;
import Transferer.App.FileSplitterMerger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * Created by Viliam on 28.1.2014.
 */
public class MainFrame {
    // tlacidlo na vyber suboru
    private JButton fileChooserButton;
    // tlacitlo na zacatie posielania
    private JButton startButton;
    // tlacidlo na pozastavenie a obnovenie posielania
    private JButton pauseResumeButton;
    private JPanel MainPanel;
    // kombo box na vyber poctu vlakien
    private JComboBox numberOfSocketsComboBox;
    // tlacidlo na vymazanie bordelu po dokonceni stahovania (je tam, lebo automaticke mazanie mi blblo a nemal som cas to riesit)
    private JButton clearBuffersButton;
    // toto reprezentuje subor ktory budeme posielat
    private static File file;
    //private FileSender fileSender;
    // private JFileChooser fileChooser;

    // FileSender zabezpecuje poslanie suboru (jedna so klienta, server sa nazyva FileReceiver)
    private volatile FileSender fileSender;

    // ci je posielanie pozastavene alebo nie
    private volatile boolean sendingPaused = false;

    // konstruktor pre GUI
    public MainFrame() {

        // nakodime co maju tlacidla robit po kliknuti
        setButtonsBehavior();
    }

    private void setButtonsBehavior() {

        // tu sa riesi vyber suboru
        // vybraty subor ukladame do instancnej premennej file
        fileChooserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser fileChooser = new JFileChooser();
                int returnVal = fileChooser.showOpenDialog((Component) e.getSource());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        file = fileChooser.getSelectedFile();
                        System.out.println("File selected: "+ file.getName());
                    } catch (Exception ex) {
                        System.err.println("Something wrong with file select!");
                    }
                }
            }
        });

        // DOLEZITE
        // zapocatie posielania suboru
        // tuto metodu si prejdi celu
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // posielanie riesime v samostatnom vlakne aby nezmrzlo GUI, lebo je to dlhotrvajuca operacia
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        if (file != null) {
                            System.out.println("Sending started by user");
                            // naparsujeme vybrany pocet vlakien cez ktory budeme posielat subor
                            int numberOfSockets = Integer.parseInt((String) numberOfSocketsComboBox.getSelectedItem());
                            System.out.println(numberOfSockets);
                            // inicializujeme fileSender z vybratym suborom a poctom vlakien
                            fileSender = FileSender.create(file, numberOfSockets);
                            // zacneme posielat
                            fileSender.send();
                        }else{
                            System.out.println("No file selected");
                        }
                    }
                // nastartujeme prave vytvorene vlakno na posielanie
                }).start();

            }
        });

        // DOLEZITE
        // co sa stane ak uzivatel chce pozastavit
        pauseResumeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(sendingPaused){

                }else{
                    if (fileSender != null) {
                        // prerusime vsetky vlakna cez ktore bezi posielanie suboru
                        fileSender.interrupt();
                        sendingPaused = true;
                    }
                }
            }
        });

        // tlacidlo na vymazanie bordelu po stahovani
        clearBuffersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                   // FileSplitterMerger.getInstance().clearRecDir();
                    FileSplitterMerger.getInstance().clearBufferDir();
                }catch (IOException ioe){
                    System.out.println("Cannot clear buffers");
                }
            }
        });
    }

    // spustac pre GUI
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        JFrame frame = new JFrame("MainFrame");
        frame.setContentPane(new MainFrame().MainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // nastavime prijmac suboru (teda server aby pocuval na preddefinovanom porte)
        FileReceiver.listen();


    }
}
