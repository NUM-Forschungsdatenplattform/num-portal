package de.vitagroup.num.integrationtesting.config;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

/**
 * test container for clamav service
 */
@Slf4j
public class ClamAVContainer extends GenericContainer<ClamAVContainer>{
    private static final String IMAGE_VERSION = "clamav/clamav:1.2";
    private static ClamAVContainer container;

    private ClamAVContainer(String image) {
        super(image);
    }

    public static ClamAVContainer getInstance() {
        if (container == null) {
            container = new ClamAVContainer(IMAGE_VERSION)
                    .withExposedPorts(3310);
        }
        return container;
    }

    @Override
    public void start() {
        log.info("----- START ClamAVContainer called ----");
        super.start();
    }
}
