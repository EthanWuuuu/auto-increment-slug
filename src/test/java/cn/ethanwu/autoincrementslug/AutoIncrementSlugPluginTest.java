package cn.ethanwu.autoincrementslug;

import cn.ethanwu.autoincrementslug.service.AISConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import run.halo.app.plugin.PluginContext;

@ExtendWith(MockitoExtension.class)
class AutoIncrementSlugPluginTest {

    @Mock
    PluginContext context;

    @InjectMocks
    AutoIncrementSlugPlugin plugin;

    @Test
    void contextLoads() {
        plugin.start();
        plugin.stop();
    }

    @Test
    void test(){
        AISConfigService service = new AISConfigService();
        System.out.println(service.generateSlug());
    }
}
