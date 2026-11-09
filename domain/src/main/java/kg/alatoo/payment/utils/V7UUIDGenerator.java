package kg.alatoo.payment.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.experimental.UtilityClass;

import java.util.UUID;

/**
 * UUID v7: упорядочены по времени, поэтому хорошо работают как первичный ключ в индексе.
 */
@UtilityClass
public class V7UUIDGenerator {
    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    public static UUID generate() {
        return GENERATOR.generate();
    }
}
