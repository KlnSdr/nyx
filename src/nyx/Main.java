package nyx;

import common.inject.api.RegisterFor;
import hades.Hades;
import hades.HadesDependencyProvider;

@RegisterFor(Main.class)
public class Main extends Hades {
    public Main(HadesDependencyProvider hadesDependencyProvider) {
        super(hadesDependencyProvider);
    }

    public static void main(String[] args) {
        startApplication(Main.class);
    }
}