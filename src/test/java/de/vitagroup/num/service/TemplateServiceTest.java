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

package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.mapper.TemplateMapper;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import java.time.OffsetDateTime;
import java.util.List;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TemplateServiceTest {

  @Mock private EhrBaseService ehrBaseService;

  @Mock private TemplateMapper templateMapper;

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks private TemplateService templateService;

  @Before
  public void setup() {
    TemplateMetaDataDto t1 = new TemplateMetaDataDto();
    t1.setTemplateId("t1");
    t1.setConcept("c1");
    t1.setArchetypeId("a1");
    t1.setCreatedOn(OffsetDateTime.now());

    when(ehrBaseService.getAllTemplatesMetadata()).thenReturn(List.of(t1));
    when(templateMapper.convertToTemplateMetadataDto(any()))
        .thenReturn(TemplateMetadataDto.builder().name("t1").build());

    UserDetails approvedUser =
        UserDetails.builder().userId("approvedUserId").approved(true).build();

    when(userDetailsService.checkIsUserApproved("approvedUserId"))
        .thenReturn(approvedUser);
  }

  @Test
  public void shouldCorrectlyRetrieveTemplateMetadata() {
    List<TemplateMetadataDto> numTemplates =
        templateService.getAllTemplatesMetadata("approvedUserId");

    assertThat(numTemplates, notNullValue());
    assertThat(numTemplates.size(), is(1));
    assertThat(numTemplates.get(0).getName(), is("t1"));
  }
}
