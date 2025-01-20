package org.poo.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.poo.fileio.CommerciantInput;

@Getter
@Setter
@NoArgsConstructor
public class Commerciant {
    private CommerciantInput commerciant;
    private int transactions;
}
