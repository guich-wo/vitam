/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmrecette.appserver.applicativetest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.google.common.base.Throwables;

/**
 * service to manage cucumber test
 */
public class ApplicativeTestService {

    /**
     * custom formatter
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * name of the package
     */
    private static final String GLUE_CODE_PACKAGE = "fr.gouv.vitam.functionaltest.cucumber";

    /**
     * flag to indicate if a test is in progress or not.
     */
    private AtomicBoolean inProgress;

    /**
     * executor to launch test in a separate thread
     */
    private Executor executor;

    /**
     * cucumber launcher
     */
    private CucumberLauncher cucumberLauncher;

    /**
     * path of the tnr report directory
     */
    private Path tnrReportDirectory;

    public ApplicativeTestService(Path tnrReportDirectory) {
        this.tnrReportDirectory = tnrReportDirectory;
        this.inProgress = new AtomicBoolean(false);
        this.executor = Executors.newSingleThreadExecutor();
        this.cucumberLauncher = new CucumberLauncher(tnrReportDirectory);
    }

    /**
     * launch cucumber test.
     *
     * @param featurePath path to the feature
     */
    public String launchCucumberTest(Path featurePath) {
        if (!Files.exists(featurePath)) {
            throw new RuntimeException("path does not exist: " + featurePath);
        }
        String fileName = String.format("report_%s.json", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        inProgress.set(true);
        executor.execute(() -> {
            List<String> arguments = cucumberLauncher.buildCucumberArgument(GLUE_CODE_PACKAGE, featurePath, fileName);
            try {
                cucumberLauncher.launchCucumberTest(arguments);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            inProgress.set(false);
        });

        return fileName;
    }

    /**
     * @return if a tnr is in progress.
     */
    public boolean inProgress() {
        return inProgress.get();
    }

    /**
     * @return the list of reports.
     */
    public List<Path> reports() throws IOException {
        return Files.list(tnrReportDirectory).collect(Collectors.toList());
    }

    /**
     * @param fileName name of the report.
     * @return stream on the report.
     * @throws IOException if the report is not found.
     */
    public InputStream readReport(String fileName) throws IOException {
        return Files.newInputStream(tnrReportDirectory.resolve(fileName));
    }

    public int synchronizedTestDirectory(Path featurePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "pull", "--rebase");
        pb.directory(featurePath.toFile());
        Process p = pb.start();
        p.waitFor();
        return p.exitValue();
    }

}
