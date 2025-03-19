package dev.jkopecky.draftbook_backend.data.tables;

import org.springframework.data.repository.CrudRepository;

public interface AuthTokenRepository extends CrudRepository<AuthToken, Integer> {
}
