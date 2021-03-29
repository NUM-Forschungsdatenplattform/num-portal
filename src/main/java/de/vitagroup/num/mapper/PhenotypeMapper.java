/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.dto.ExpressionDto;
import de.vitagroup.num.domain.dto.PhenotypeDto;
import de.vitagroup.num.service.UserService;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PhenotypeMapper {

  private final ModelMapper modelMapper;
  private final UserService userService;

  @PostConstruct
  public void initialize() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
  }

  public PhenotypeDto convertToDto(Phenotype phenotype) {
    PhenotypeDto phenotypeDto = modelMapper.map(phenotype, PhenotypeDto.class);

    if (phenotype.getOwner() != null) {
      User owner = userService.getUserById(phenotype.getOwner().getUserId(), false);
      phenotypeDto.setOwner(owner);
    }

    return phenotypeDto;
  }

  public Phenotype convertToEntity(PhenotypeDto phenotypeDto) {
    Phenotype phenotype = modelMapper.map(phenotypeDto, Phenotype.class);
    phenotype.setId(null);
    return phenotype;
  }

  public Phenotype convertToEntity(ExpressionDto expressionDto) {
    return modelMapper.map(expressionDto, Phenotype.class);
  }
}
