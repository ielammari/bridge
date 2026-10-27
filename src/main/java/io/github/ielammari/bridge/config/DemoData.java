package io.github.ielammari.bridge.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Maintains the demonstration data. While bridge.demo.reset is true, each start
 * reruns V17's script outside Flyway, bringing its dates to the present. Every
 * seeded CV path missing from disk then receives {@code demo/cv.pdf}. Off under
 * the prod profile.
 */
@Component
@Order(1)
public class DemoData implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoData.class);

	private static final String SCRIPT = "db/migration/V17__seed_demo_data.sql";
	private static final Pattern DEMO_PATH = Pattern.compile("\\d+/demo-[a-z-]+\\.pdf");
	private static final ClassPathResource PLACEHOLDER = new ClassPathResource("demo/cv.pdf");

	private final JdbcTemplate jdbc;
	private final TransactionTemplate transaction;
	private final boolean enabled;
	private final boolean reset;
	private final Path root;

	public DemoData(JdbcTemplate jdbc, PlatformTransactionManager transactions,
			@Value("${bridge.demo.enabled}") boolean enabled,
			@Value("${bridge.demo.reset}") boolean reset,
			@Value("${bridge.storage.cv-dir}") String cvDir) {
		this.jdbc = jdbc;
		this.transaction = new TransactionTemplate(transactions);
		this.enabled = enabled;
		this.reset = reset;
		this.root = Path.of(cvDir).toAbsolutePath().normalize();
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!enabled) {
			return;
		}
		if (reset) {
			log.warn("BRIDGE_DEMO_RESET is set: emptying the database and reloading the demonstration data");
			reload();
		}
		writeCvFiles();
	}

	private void reload() {
		String script;
		try (InputStream in = new ClassPathResource(SCRIPT).getInputStream()) {
			script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read " + SCRIPT, e);
		}
		transaction.executeWithoutResult(status -> jdbc.execute(script));
		log.info("Reloaded the demonstration data");
	}

	private void writeCvFiles() {
		List<String> paths = jdbc.queryForList(
				"SELECT chemin FROM cv UNION SELECT cv_joint FROM candidature", String.class);
		int written = 0;
		for (String path : paths) {
			Path target = root.resolve(path).normalize();
			if (!DEMO_PATH.matcher(path).matches() || Files.exists(target)) {
				continue;
			}
			try (InputStream in = PLACEHOLDER.getInputStream()) {
				Files.createDirectories(target.getParent());
				Files.copy(in, target);
				written++;
			} catch (IOException e) {
				log.warn("Could not write demonstration CV {}", path, e);
			}
		}
		if (written > 0) {
			log.info("Wrote {} demonstration CV files under {}", written, root);
		}
	}

}
