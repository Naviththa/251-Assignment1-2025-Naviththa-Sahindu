package com.texteditor;

import javax.swing.*;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.yaml.snakeyaml.Yaml;

public class App {

    private JFrame frame;
    private RSyntaxTextArea textArea;
    private Path currentFile;

    // user defaults from YAML
    private String defaultFontName = "Arial";
    private int defaultFontSize = 14;
    private Color defaultColor = Color.BLACK;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().start());
    }

    private void start() {
        loadYamlDefaults(); // optional config.yaml

        frame = new JFrame("Text Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 650);
        frame.setLocationRelativeTo(null);

        textArea = new RSyntaxTextArea();
        textArea.setCodeFoldingEnabled(true);
        textArea.setFont(new Font(defaultFontName, Font.PLAIN, defaultFontSize));
        textArea.setForeground(defaultColor);

        RTextScrollPane sp = new RTextScrollPane(textArea);
        frame.add(sp, BorderLayout.CENTER);

        frame.setJMenuBar(buildMenuBar());
        frame.setVisible(true);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        // File
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem miNew = new JMenuItem("New");
        JMenuItem miOpen = new JMenuItem("Open...");
        JMenuItem miSave = new JMenuItem("Save");
        JMenuItem miSaveAs = new JMenuItem("Save As...");
        JMenuItem miExportPdf = new JMenuItem("Export as PDF...");
        JMenuItem miPrint = new JMenuItem("Print...");
        JMenuItem miExit = new JMenuItem("Exit");

        miNew.addActionListener(e -> doNew());
        miOpen.addActionListener(e -> doOpen());
        miSave.addActionListener(e -> doSave(false));
        miSaveAs.addActionListener(e -> doSave(true));
        miExportPdf.addActionListener(e -> doExportPdf());
        miPrint.addActionListener(e -> doPrint());
        miExit.addActionListener(e -> frame.dispose());

        file.add(miNew); file.add(miOpen);
        file.add(miSave); file.add(miSaveAs);
        file.addSeparator();
        file.add(miExportPdf); file.add(miPrint);
        file.addSeparator();
        file.add(miExit);

        // Edit
        JMenu edit = new JMenu("Edit");
        JMenuItem miCut = new JMenuItem("Cut");
        JMenuItem miCopy = new JMenuItem("Copy");
        JMenuItem miPaste = new JMenuItem("Paste");
        JMenuItem miSelectAll = new JMenuItem("Select All");
        miCut.addActionListener(e -> textArea.cut());
        miCopy.addActionListener(e -> textArea.copy());
        miPaste.addActionListener(e -> textArea.paste());
        miSelectAll.addActionListener(e -> textArea.selectAll());
        edit.add(miCut); edit.add(miCopy); edit.add(miPaste); edit.add(miSelectAll);

        // Search
        JMenu search = new JMenu("Search");
        JMenuItem miFind = new JMenuItem("Find...");
        miFind.addActionListener(e -> doFind());
        search.add(miFind);

        // View (Time & Date insert)
        JMenu view = new JMenu("View");
        JMenuItem miTimeDate = new JMenuItem("Insert Time & Date at Top");
        miTimeDate.addActionListener(e -> insertTimeAndDateAtTop());
        view.add(miTimeDate);

        // Help
        JMenu help = new JMenu("Help");
        JMenuItem miAbout = new JMenuItem("About");
        miAbout.addActionListener(e -> showAbout());
        help.add(miAbout);

        mb.add(file); mb.add(edit); mb.add(search); mb.add(view); mb.add(help);
        return mb;
    }

    private void doNew() {
        if (confirmLoseChanges()) {
            textArea.setText("");
            currentFile = null;
            frame.setTitle("Text Editor - Untitled");
            applySyntax(null);
        }
    }

    private boolean confirmLoseChanges() {
        return true;
    }

    private void doOpen() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            String text;
            String name = f.getName().toLowerCase();
            if (name.endsWith(".rtf")) {
                text = readRTF(f);
            } else if (name.endsWith(".odt")) {
                text = readODT(f);
            } else {
                text = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            }
            textArea.setText(text);
            currentFile = f.toPath();
            frame.setTitle("Text Editor - " + f.getName());
            applySyntax(f.getName());
        } catch (Exception ex) {
            showError("Open failed: " + ex.getMessage());
        }
    }

    private void doSave(boolean saveAs) {
        try {
            Path out = currentFile;
            if (saveAs || out == null) {
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
                out = fc.getSelectedFile().toPath();
            }
            Files.writeString(out, textArea.getText(), StandardCharsets.UTF_8);
            currentFile = out;
            frame.setTitle("Text Editor - " + out.getFileName());
        } catch (Exception ex) {
            showError("Save failed: " + ex.getMessage());
        }
    }

    private void doExportPdf() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("document.pdf"));
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);

            float margin = 40;
            float leading = 14;
            float x = margin;
            float y = page.getMediaBox().getHeight() - margin;
            cs.newLineAtOffset(x, y);

            // naive wrap: 90 chars per line
            for (String line : textArea.getText().split("\r?\n")) {
                while (line.length() > 0) {
                    int len = Math.min(90, line.length());
                    String part = line.substring(0, len);
                    cs.showText(part);
                    cs.newLineAtOffset(0, -leading);
                    line = line.substring(len);
                }
                cs.newLineAtOffset(0, -leading);
            }

            cs.endText();
            cs.close();

            doc.save(out);
            JOptionPane.showMessageDialog(frame, "Saved PDF: " + out.getAbsolutePath());
        } catch (Exception ex) {
            showError("PDF export failed: " + ex.getMessage());
        }
    }

    private void doPrint() {
        try {
            if (!textArea.print()) {
                JOptionPane.showMessageDialog(frame, "Print canceled.");
            }
        } catch (Exception ex) {
            showError("Print error: " + ex.getMessage());
        }
    }

    private void doFind() {
        String q = JOptionPane.showInputDialog(frame, "Find (single word):");
        if (q == null || q.isEmpty()) return;
        String content = textArea.getText();
        int from = Math.max(0, textArea.getCaretPosition());
        int pos = content.indexOf(q, from);
        if (pos < 0) pos = content.indexOf(q); // wrap around
        if (pos >= 0) {
            textArea.requestFocus();
            textArea.select(pos, pos + q.length());
        } else {
            JOptionPane.showMessageDialog(frame, "Not found.");
        }
    }

    private void insertTimeAndDateAtTop() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        textArea.insert("[T&D] " + LocalDateTime.now().format(fmt) + System.lineSeparator(), 0);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(frame,
                "Text Editor\nAuthors: Naviththa Bathisa Madampage (25013309), Sahindu (25015527)",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---- Helpers ----

    private String readRTF(File f) throws IOException, BadLocationException {
        RTFEditorKit rtf = new RTFEditorKit();
        Document doc = rtf.createDefaultDocument();
        try (InputStream is = new FileInputStream(f)) {
            rtf.read(is, doc, 0);
        }
        return doc.getText(0, doc.getLength());
    }

    private String readODT(File f) throws Exception {
        // Try Simple API first
        try {
            TextDocument odt = TextDocument.loadDocument(f);
            return odt.getContentRoot().getTextContent();
        } catch (Throwable ignore) {
            // Fallback to ODFDOM
            OdfTextDocument odt2 = OdfTextDocument.loadDocument(f);
            return odt2.getContentRoot().getTextContent();
        }
    }

    private void applySyntax(String name) {
        if (name == null) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            return;
        }
        String p = name.toLowerCase();
        if (p.endsWith(".java")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        else if (p.endsWith(".py")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        else if (p.endsWith(".js")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        else textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
    }

    private void loadYamlDefaults() {
        Path yaml = Paths.get("config.yaml");
        if (!Files.exists(yaml)) return;
        try (InputStream in = Files.newInputStream(yaml)) {
            Map<String, Object> m = new Yaml().load(in);
            if (m == null) return;
            if (m.containsKey("defaultFont")) defaultFontName = String.valueOf(m.get("defaultFont"));
            if (m.containsKey("defaultFontSize")) defaultFontSize = Integer.parseInt(String.valueOf(m.get("defaultFontSize")));
            if (m.containsKey("defaultColor")) {
                String c = String.valueOf(m.get("defaultColor")).toLowerCase();
                switch (c) {
                    case "black": defaultColor = Color.BLACK; break;
                    case "darkgray": defaultColor = Color.DARK_GRAY; break;
                    case "blue": defaultColor = Color.BLUE; break;
                    case "red": defaultColor = Color.RED; break;
                }
            }
        } catch (Exception ignored) { }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
