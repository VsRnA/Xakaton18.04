package com.prodforge.game.application.gameevent;

import com.prodforge.game.domain.gameevent.GameEventLog;
import com.prodforge.game.domain.gameevent.GameEventLogQuery;
import com.prodforge.game.domain.gameevent.GameEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameEventLogServiceImpl implements GameEventLogService {

    private final GameEventLogRepository repository;

    @Override
    @Transactional
    public void log(UUID roomId, String eventType, Map<String, Object> details) {
        repository.save(new GameEventLog(roomId, eventType, details));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameEventLog> getEvents(UUID roomId) {
        return repository.list(GameEventLogQuery.byRoom(roomId));
    }
}
