package ru.sidorenko.java_2;

public enum ListKind {

    LINKED("linked", "собственный связный список") {
        @Override
        public SortableList create() {
            return new LinkedStringList();
        }

        @Override
        public double expectedSteps(SortSettings settings, double elapsedMillis) {
            return parallelSteps(settings, elapsedMillis);
        }
    },

    ARRAY("array", "Collections.synchronizedList(ArrayList)") {
        @Override
        public SortableList create() {
            return new ArrayStringList();
        }

        @Override
        public double expectedSteps(SortSettings settings, double elapsedMillis) {
            double parallel = parallelSteps(settings, elapsedMillis);
            if (settings.insideStepMillis() == 0) {
                return parallel;
            }
            return Math.min(parallel, elapsedMillis / settings.insideStepMillis());
        }
    };

    private final String key;
    private final String title;

    ListKind(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public abstract SortableList create();

    public abstract double expectedSteps(SortSettings settings, double elapsedMillis);

    public String key() {
        return key;
    }

    public String title() {
        return title;
    }

    public static ListKind parse(String value) {
        for (ListKind kind : values()) {
            if (kind.key.equalsIgnoreCase(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException(
                "Неизвестная реализация списка: " + value
        );
    }

    private static double parallelSteps(SortSettings settings, double elapsedMillis) {
        long period = settings.betweenStepsMillis() + settings.insideStepMillis();
        if (period == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return settings.threads() * elapsedMillis / period;
    }
}
