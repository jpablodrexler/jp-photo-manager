package com.jpablodrexler.photomanager.domain.port.out;

import java.io.IOException;

public interface HashCalculatorPort {

    String computeSha256(String filePath) throws IOException;
}
