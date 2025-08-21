package com.texteditor;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.rtf.RTFEditorKit;

public class App extends JFrame {
  private final JTextArea area = new JTextArea();

  public App() {
    setTitle("251 Text Editor");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(900, 700);
    area.setFont(new Font("Consolas", Font.PLAIN, 14));
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    setJMenuBar(menu());
    add(new JScrollPane(area), BorderLayout.CENTER);
    setLocationRelativeTo(null);
    setVisible(true);
  }

  private JMenuBar menu() {
    var bar = new JMenuBar();
    var file = new JMenu("File");
    file.add(item("New", e -> area.setText("")));
    file.add(item("Open (.txt/.rtf/.odt)", e -> openFile()));
    file.add(item("Save (.txt)", e -> saveFile()));
    file.addSeparator();
    file.add(item("Exit", e -> dispose()));
    bar.add(file);
    return bar;
  }

  private JMenuItem item(String name, java.awt.event.ActionListener a) {
    var it = new JMenuItem(name);
    it.addActionListener(a);
    return it;
  }

  private void openFile() {
    var fc = new JFileChooser();
    fc.setFileFilter(new FileNameExtensionFilter("Text/RTF/ODT", "txt","rtf","odt"));
    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      var f = fc.getSelectedFile();
      try {
        var name = f.getName().toLowerCase();
        if (name.endsWith(".rtf")) {
          var kit = new RTFEditorKit();
          var doc = kit.createDefaultDocument();
          try (var in = new FileInputStream(f)) { kit.read(in, doc, 0); }
          area.setText(doc.getText(0, doc.getLength()));
        } else if (name.endsWith(".odt")) {
          area.setText("[TODO ODT] " + f.getAbsolutePath());
        } else {
          area.setText(Files.readString(f.toPath()));
        }
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void saveFile() {
    var fc = new JFileChooser();
    fc.setSelectedFile(new File("document.txt"));
    if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      try { Files.writeString(fc.getSelectedFile().toPath(), area.getText()); }
      catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
    }
  }

  public static void main(String[] args) { SwingUtilities.invokeLater(App::new); }
}
