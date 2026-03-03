package org.example.expert.domain.todo.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.dto.response.QTodoSearchResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.expert.domain.comment.entity.QComment.comment;
import static org.example.expert.domain.manager.entity.QManager.manager;
import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class TodoRepositoryImpl implements TodoRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        Todo result = jpaQueryFactory
                .selectFrom(todo)
                .leftJoin(todo.user, user).fetchJoin()  // N+1 방지: fetchJoin 사용
                .where(todo.id.eq(todoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(String title, LocalDateTime startDate, LocalDateTime endDate,
                                                String managerNickname, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // 제목 검색 (부분 일치)
        if (title != null && !title.isEmpty()) {
            builder.and(todo.title.containsIgnoreCase(title));
        }

        // 생성일 범위 검색
        if (startDate != null && endDate != null) {
            builder.and(todo.createdAt.between(startDate, endDate));
        }

        // 담당자 닉네임 검색 (부분 일치)
        if (managerNickname != null && !managerNickname.isEmpty()) {
            builder.and(manager.user.nickName.containsIgnoreCase(managerNickname));
        }

        // Projections를 활용하여 필요한 필드만 조회
        List<TodoSearchResponse> results = jpaQueryFactory
                .select(new QTodoSearchResponse(
                        todo.id,
                        todo.title,
                        manager.count(),
                        comment.count(),
                        todo.createdAt
                ))
                .from(todo)
                .leftJoin(todo.managers, manager)
                .leftJoin(todo.comments, comment)
                .where(builder)
                .groupBy(todo.id, todo.title, todo.createdAt)
                .orderBy(new OrderSpecifier<>(Order.DESC, todo.createdAt))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 조회
        Long total = jpaQueryFactory
                .select(todo.id)
                .from(todo)
                .leftJoin(todo.managers, manager)
                .where(builder)
                .groupBy(todo.id)
                .fetch()
                .stream()
                .count();

        return new PageImpl<>(results, pageable, total);
    }
}
