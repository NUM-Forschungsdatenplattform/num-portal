package org.highmed.numportal.service.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ExceptionDto {

  int id;
  List<String> argumentsList;
}
