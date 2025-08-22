package com.texteditor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class App extends JFrame {

    private final RSyntaxTextArea editor = new RSyntaxTextArea(30, 100);
    private File currentFile = null;
    private String lastFind = null;
    private int lastFindPos = 0;

    public App() {
        setTitle("251 Text Editor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);

        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        editor.setFont(new Font("Consolas", Font.PLAIN, 14));

        RTextScrollPane sp = new RTextScrollPane(editor);
        sp.setFoldIndicatorEnabled(true);
        add(sp, BorderLayout.CENTER);

        setJMenuBar(buildMenuBar());
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        // File
        JMenu mFile = new JMenu("File");
        mFile.add(new JMenuItem(new AbstractAction("New") {
            public void actionPerformed(ActionEvent e) { doNew(); }
        }));
        mFile.add(new JMenuItem(new AbstractAction("Open (.txt/.rtf/.odt)") {
            public void actionPerformed(ActionEvent e) { doOpen(); }
        }));
        mFile.add(new JMenuItem(new AbstractAction("Save (.txt)") {
            public void actionPerformed(ActionEvent e) { doSave(); }
        }));
        mFile.addSeparator();
        mFile.add(new JMenuItem(new AbstractAction("Print") {
            public void actionPerformed(ActionEvent e) { doPrint(); }
        }));
        mFile.add(new JMenuItem(new AbstractAction("Export PDF") {
            public void actionPerformed(ActionEvent e) { doExportPdf(); }
        }));
        mFile.addSeparator();
        mFile.add(new JMenuItem(new AbstractAction("Exit") {
            public void actionPerformed(ActionEvent e) { dispose(); }
        }));
        bar.add(mFile);

        // Edit
        JMenu mEdit = new JMenu("Edit");
        mEdit.add(new JMenuItem(new DefaultEditorKit.CopyAction() {{ putValue(NAME,"Copy"); }}));
        mEdit.add(new JMenuItem(new DefaultEditorKit.PasteAction(){{ putValue(NAME,"Paste"); }}));
        mEdit.add(new JMenuItem(new DefaultEditorKit.CutAction()  {{ putValue(NAME,"Cut"); }}));
        mEdit.add(new JMenuItem(new AbstractAction("Clear") {
            public void actionPerformed(ActionEvent e) { editor.setText(""); }
        }));
        mEdit.addSeparator();
        mEdit.add(new JMenuItem(new AbstractAction("Time & Date") {
            public void actionPerformed(ActionEvent e) { insertTimeDate(); }
        }));
        bar.add(mEdit);

        // Search
        JMenu mSearch = new JMenu("Search");
        mSearch.add(new JMenuItem(new AbstractAction("Find...") {
            public void actionPerformed(ActionEvent e) { doFind(); }
        }));
        mSearch.add(new JMenuItem(new AbstractAction("Find Next") {
            public void actionPerformed(ActionEvent e) { doFindNext(); }
        }));
        bar.add(mSearch);

        // Help
        JMenu mHelp = new JMenu("Help");
        mHelp.add(new JMenuItem(new AbstractAction("About") {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(App.this,
                        "Text Editor for 251 Assignment\nAuthors: Naviththa (25013309), Sahindu (25015527)",
                        "About", JOptionPane.INFORMATION_MESSAGE);
            }
        }));
        bar.add(mHelp);

        return bar;
    }

    // === File actions ===
    private void doNew() { editor.setText(""); currentFile = null; setTitle("251 Text Editor"); }

    private void doOpen() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Text/RTF/ODT", "txt", "rtf", "odt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                String name = f.getName().toLowerCase();
                String text;
                if (name.endsWith(".rtf")) {
                    text = loadRtf(f);
                } else if (name.endsWith(".odt")) {
                    text = loadWithTika(f); // ODT (and others) via Apache Tika
                } else {
                    text = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                }
                editor.setText(text);
                editor.setCaretPosition(0);
                currentFile = f;
                setTitle("251 Text Editor — " + f.getName());
                applySyntaxForFilename(name);
            } catch (Exception ex) {
                showErr(ex);
            }
        }
    }

    private void doSave() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(currentFile != null ? currentFile : new File("document.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                Files.writeString(f.toPath(), editor.getText(), StandardCharsets.UTF_8);
                currentFile = f;
                setTitle("251 Text Editor — " + f.getName());
            } catch (IOException ex) { showErr(ex); }
        }
    }

    private void doPrint() {
        try { editor.print(); }
        catch (Exception ex) { showErr(ex); }
    }

    private void doExportPdf() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("export.pdf"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();
            try {
                exportPdfSimple(editor.getText(), out);
                JOptionPane.showMessageDialog(this, "PDF exported:\n" + out.getAbsolutePath());
            } catch (Exception ex) { showErr(ex); }
        }
    }

    // === Search ===
    private void doFind() {
        String input = JOptionPane.showInputDialog(this, "Find:");
        if (input == null || input.isEmpty()) return;
        lastFind = input;
        lastFindPos = editor.getCaretPosition();
        findFrom(lastFindPos);
    }

    private void doFindNext() {
        if (lastFind == null || lastFind.isEmpty()) { doFind(); return; }
        lastFindPos = editor.getCaretPosition();
        findFrom(lastFindPos);
    }

    private void findFrom(int start) {
        String hay = editor.getText();
        int idx = hay.indexOf(lastFind, Math.max(0, start));
        if (idx >= 0) {
            editor.requestFocusInWindow();
            editor.select(idx, idx + lastFind.length());
        } else {
            JOptionPane.showMessageDialog(this, "Not found.");
        }
    }

    // === Helpers ===
    private void insertTimeDate() {
        String ts = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        try {
            Document doc = editor.getDocument();
            int pos = editor.getCaretPosition();
            doc.insertString(pos, ts, null);
        } catch (BadLocationException e) { showErr(e); }
    }

    private void applySyntaxForFilename(String name) {
        String style = SyntaxConstants.SYNTAX_STYLE_NONE;
        if (name.endsWith(".java")) style = SyntaxConstants.SYNTAX_STYLE_JAVA;
        else if (name.endsWith(".py")) style = SyntaxConstants.SYNTAX_STYLE_PYTHON;
        else if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".hpp")) style = SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
        else if (name.endsWith(".js")) style = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
        editor.setSyntaxEditingStyle(style);
    }

    private String loadRtf(File f) throws Exception {
        RTFEditorKit kit = new RTFEditorKit();
        javax.swing.text.Document doc = kit.createDefaultDocument();
        try (InputStream in = new FileInputStream(f)) { kit.read(in, doc, 0); }
        return doc.getText(0, doc.getLength());
    }

    /** ODT and many other formats via Apache Tika */
    private String loadWithTika(File f) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1); // unlimited
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        try (InputStream stream = new FileInputStream(f)) {
            parser.parse(stream, handler, metadata, context);
            return handler.toString();
        }
    }

    /** Very simple PDF export with basic wrapping */
    private void exportPdfSimple(String text, File outFile) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 50;
            float yStart = page.getMediaBox().getUpperRightY() - margin;

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.setLeading(14.5f);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(margin, yStart);

            float y = yStart;
            for (String line : text.split("\\R", -1)) {
                for (String chunk : wrapLine(line, 100)) {
                    cs.showText(chunk == null ? "" : chunk);
                    cs.newLine();
                    y -= 14.5f;
                    if (y <= margin) {
                        cs.endText(); cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        cs.setLeading(14.5f);
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 12);
                        cs.newLineAtOffset(margin, yStart);
                        y = yStart;
                    }
                }
            }
            cs.endText(); cs.close();
            doc.save(outFile);
        }
    }

    private static List<String> wrapLine(String line, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (line == null || line.isEmpty()) { lines.add(""); return lines; }
        int i = 0;
        while (i < line.length()) {
            int end = Math.min(line.length(), i + maxChars);
            lines.add(line.substring(i, end));
            i = end;
        }
        return lines;
    }

    private void showErr(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
    }
}
