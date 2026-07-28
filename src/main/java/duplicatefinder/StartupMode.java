package duplicatefinder;

/** Startmodus der Anwendung: grafische Oberfläche oder Kommandozeile. */
enum StartupMode {

    GUI {
        @Override void start(String[] args) { DuplicateFinderGUI.launch(); }
    },
    CLI {
        @Override void start(String[] args) { DuplicateFinderCLI.run(args); }
    };

    abstract void start(String[] args);

    static StartupMode resolve(String[] args) {
        return args.length == 0 ? GUI : CLI;
    }
}