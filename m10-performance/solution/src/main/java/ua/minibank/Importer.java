package ua.minibank;

import java.nio.file.Path;

public interface Importer {

    ImportResult importFile(Path csv);
}
