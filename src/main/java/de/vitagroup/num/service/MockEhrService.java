package de.vitagroup.num.service;

import de.vitagroup.num.domain.Aql;
import org.springframework.stereotype.Service;

import java.util.Set;

//TODO: implement service that calls open ehr
@Service
public class MockEhrService {


    public Set<String> getAllPatientIds() {
        return Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "25", "256");
    }

    public Set<String> executeAql(Aql aql){
        return Set.of("1", "2", "3");
    }

}
