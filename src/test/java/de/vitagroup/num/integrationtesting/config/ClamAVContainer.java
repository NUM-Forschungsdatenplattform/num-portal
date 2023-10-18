package de.vitagroup.num.integrationtesting.config;

import com.github.dockerjava.api.command.InspectContainerResponse;
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
                    .withExposedPorts(3310)
                    .withAccessToHost(true);
        }
        return container;
    }

    @Override
    public void start() {
        log.info("----- START ClamAVContainer called ----");
        super.start();
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        log.info("ClamAVContainer started {} ", containerInfo);
    }
}
