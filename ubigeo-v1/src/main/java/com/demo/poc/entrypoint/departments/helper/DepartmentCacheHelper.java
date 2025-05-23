package com.demo.poc.entrypoint.departments.helper;

import com.demo.poc.commons.custom.cache.RedisManager;
import com.demo.poc.commons.custom.enums.UbigeoType;
import com.demo.poc.commons.custom.properties.ApplicationProperties;
import com.demo.poc.entrypoint.departments.repository.DepartmentRepository;
import com.demo.poc.entrypoint.departments.repository.entity.DepartmentEntity;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentCacheHelper {

  private final ApplicationProperties properties;
  private final ReactiveRedisTemplate<String, Object> redisTemplate;
  private final RedisManager redisManager;
  private final DepartmentRepository departmentRepository;

  public Flux<DepartmentEntity> findAllDepartments() {
    return redisManager.isRedisAvailable()
        .filter(isRedisAvailable -> isRedisAvailable)
        .flatMapMany(isRedisAvailable -> getDepartmentsFromCacheIfPresent())
        .switchIfEmpty(departmentRepository.findAll());
  }

  private Flux<DepartmentEntity> getDepartmentsFromCacheIfPresent() {
    return redisTemplate.opsForSet()
        .scan(buildCacheKey())
        .cast(DepartmentEntity.class)
        .switchIfEmpty(Flux.defer(() -> departmentRepository.findAll()
            .flatMap(department -> redisTemplate.opsForSet().add(buildCacheKey(), department)
                .thenReturn(department))));
  }

  private String buildCacheKey() {
    return properties.searchCache(UbigeoType.DEPARTMENTS.getLabel()).getKeyPrefix();
  }

}
