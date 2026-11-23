package com.xinzhe.projectmentor.claim;

import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** Measures the production matcher, not claim extraction, retrieval or LLM output. */
class ClaimEvaluationTests {
    private static final Path EVALUATION = Path.of("..", "..", "evaluation");

    @Test
    void reportsBaselineWithoutTreatingSyntheticAccuracyAsAReleaseGate() throws IOException {
        var labels = Files.readAllLines(EVALUATION.resolve("claim-labels.tsv"));
        var matcher = new ClaimEvidenceMatcher(new ClaimTextSanitizer());
        var matcherStatuses = ClaimEvidenceStatus.values();
        int[][] confusion = new int[matcherStatuses.length][matcherStatuses.length];
        var seen = new HashSet<String>();
        var errors = new StringBuilder();
        assertThat(labels.get(0)).isEqualTo("project\tscope\texpected\tcategory\tclaim\trationale");
        for (String line : labels.subList(1, labels.size())) {
            String[] fields = line.split("\t", -1);
            assertThat(fields).hasSize(6).doesNotContain("");
            assertThat(seen.add(fields[0] + "/" + fields[1] + "/" + fields[4])).isTrue();
            ClaimEvidenceStatus expected = ClaimEvidenceStatus.valueOf(fields[2]);
            var claim = ClaimEvidenceItemVO.builder()
                    .claimText(fields[4]).sourceType("PROJECT_DESCRIPTION").sourceSnippet(fields[4])
                    .category(ClaimCategory.valueOf(fields[3])).build();
            ClaimEvidenceStatus actual = matcher.match(claim, projectFiles(fields[0], fields[1])).getStatus();
            confusion[expected.ordinal()][actual.ordinal()]++;
            if (expected != actual) {
                errors.append("- %s (%s): **%s → %s** — %s. Label rationale: %s%n"
                        .formatted(fields[0], fields[1], expected, actual, fields[4], fields[5]));
            }
        }
        assertThat(seen).hasSizeGreaterThanOrEqualTo(50);
        var summary = new StringBuilder("# Synthetic Claim matcher baseline\n\n");
        summary.append("Rows are human-authored expected labels; columns are actual matcher statuses.\n\n");
        summary.append("| Expected / actual |");
        for (var status : matcherStatuses) summary.append(" ").append(status).append(" |");
        summary.append(" Precision | Recall | False-positive rate |\n| --- |")
                .append(" ---: |".repeat(matcherStatuses.length + 3)).append('\n');
        int correct = 0;
        for (var status : matcherStatuses) {
            int i = status.ordinal();
            int truePositive = confusion[i][i];
            int expectedCount = 0;
            int actualCount = 0;
            summary.append("| ").append(status).append(" |");
            for (int j = 0; j < matcherStatuses.length; j++) {
                expectedCount += confusion[i][j];
                actualCount += confusion[j][i];
                summary.append(" ").append(confusion[i][j]).append(" |");
            }
            assertThat(expectedCount).as("expected cases for %s", status).isPositive();
            correct += truePositive;
            summary.append(" ").append(rate(truePositive, actualCount)).append(" | ")
                    .append(rate(truePositive, expectedCount)).append(" | ")
                    .append(rate(actualCount - truePositive, seen.size() - expectedCount)).append(" |\n");
        }
        summary.append("\nOverall accuracy: ").append(rate(correct, seen.size()))
                .append(" (").append(correct).append('/').append(seen.size()).append(").\n\n");
        System.out.print(summary);
        summary.append("## Mismatches\n\n").append(errors);
        Path report = Path.of("target", "claim-evaluation.md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, summary);
    }

    private static String rate(int count, int total) {
        return total == 0 ? "n/a" : String.format(Locale.ROOT, "%.4f", (double) count / total);
    }

    private static List<ProjectFile> projectFiles(String project, String scope) throws IOException {
        assertThat(scope).isIn("all", "readme");
        Path root = EVALUATION.resolve("fixtures").resolve(project);
        try (var walk = Files.walk(root)) {
            var paths = walk.filter(Files::isRegularFile)
                    .filter(path -> "all".equals(scope) || path.equals(root.resolve("README.md")))
                    .sorted()
                    .toList();
            assertThat(paths).isNotEmpty();
            var files = new java.util.ArrayList<ProjectFile>(paths.size());
            for (Path path : paths) {
                var file = new ProjectFile();
                file.setId((long) files.size() + 1);
                file.setFilePath(root.relativize(path).toString().replace('\\', '/'));
                file.setFileType(path.equals(root.resolve("README.md")) ? "README" : "CODE");
                file.setContent(Files.readString(path));
                files.add(file);
            }
            return files;
        }
    }
}
