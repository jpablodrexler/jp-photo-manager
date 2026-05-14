package com.jpablodrexler.photomanager.domain.port.out;

import java.io.IOException;

public interface HashCalculatorPort {
    String computeHash(String filePath) throws IOException;
}
