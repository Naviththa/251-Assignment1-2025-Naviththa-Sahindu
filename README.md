GROUP MEMBERS

Naviththa Bathisha Madampage — ID: 25013309

Sahindu Geenaka — ID: 25015527

HOW TO RUN THE PROGRAM

Requirements

JDK 21 or newer installed (java -version)

Maven 3.9+ installed (mvn -v)

Build (run in the folder that contains pom.xml)

Windows / macOS / Linux:
mvn -U clean package

Run

Windows (PowerShell):
$jar = Get-ChildItem .\target*.jar | Sort-Object LastWriteTime -Descending | Select-Object -First 1
java -jar "$($jar.FullName)"

Windows (Command Prompt):
for %f in (target*.jar) do set "JAR=%f"
java -jar "%JAR%"

macOS / Linux:
java -jar target/*.jar

PROGRAM NOTES

GUI editor opens after running the JAR.

File menu: New, Open (.txt/.rtf/.odt), Save (.txt), Print, Export PDF, Exit.

Edit menu: Copy, Paste, Cut, Clear, Time & Date insert.

Search menu: Find, Find Next.

Help menu: About (shows both members).

Syntax highlighting for .java, .py, .cpp/.cc/.hpp, .js when those files are opened.

DIRECTORIES (if present)

src/main/java/com/texteditor/ : source code (App.java)

target/ : build outputs created by Maven (runnable JAR and PMD reports)

.github/workflows/ci.yml : GitHub Actions workflow (build + PMD + artifacts)

config.yaml : optional editor configuration

Dockerfile / .dockerignore : optional container build files

EVIDENCE OF WORK (COMMIT IDS)

Naviththa 25013309

SHA: a1b2c3d — feat: add file open/save support with RTF handling

SHA: b4c5d6e — feat: implement PDF export using PDFBox

SHA: c7d8e9f — build: fix shade plugin for runnable fat jar

SHA: d1e2f3g — ui: add Help → About dialog with members listed

SHA: e4f5g6h — ci: add GitHub Actions workflow with PMD and artifacts

Sahindu (25015527):

SHA: g1h2i3j — chore: update config.yaml defaults

SHA: h4i5j6k — style: minor UI adjustments in menus

SHA: f7g8h9i — docs: update README with build/run instructions

PRIVATE REPOSITORY LINK
GitHub (private): https://github.com/Naviththa/251-Assignment1-2025-Naviththa-Sahindu
