package com.prodforge.game.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public final class GameErrorMessages {

    private GameErrorMessages() {}

    // === Авторизация ===
    public static final String AUTH_BEARER_REQUIRED = "требуется bearer-токен";
    public static final String AUTH_TOKEN_INVALID = "токен недействителен или истёк";
    public static final String AUTH_ADMIN_REQUIRED = "доступ запрещён: требуется роль администратора";

    // === Игровая комната ===
    public static final String ROOM_NOT_ACCEPTING = "Комната не принимает игроков";
    public static final String ROOM_FULL = "Комната заполнена";
    public static final String ROOM_PARTICIPANT_ALREADY_JOINED = "Пользователь уже присоединился к этой комнате";
    public static final String ROOM_ONLY_WAITING_CAN_CANCEL = "Отменить можно только комнаты в статусе WAITING";
    public static final String ROOM_NO_SUITABLE_FOUND = "Подходящая комната в статусе WAITING не найдена для заданных параметров";
    public static final String ROOM_CONFIG_HAS_ERRORS =
            "Конфигурация комнаты содержит ошибки. Установите confirmWarnings=true для принудительного создания.";

    public static String insufficientBalanceForEntry(BigDecimal required) {
        return "Недостаточно бонусных баллов для входа в комнату. Необходимо: " + required;
    }

    // === Буст ===
    public static final String BOOST_NOT_ENABLED = "Буст не включён в этой комнате";
    public static final String BOOST_WRONG_ROUND_STATUS = "Буст можно купить только во время активного раунда";
    public static final String BOOST_ALREADY_USED = "Буст уже использован в раунде 1 — разрешён только один буст за игру";
    public static final String BOOST_ALREADY_PURCHASED_THIS_ROUND = "Буст уже куплен в этом раунде";

    public static String insufficientBalanceForBoost(BigDecimal required) {
        return "Недостаточно баллов для покупки буста. Необходимо: " + required;
    }

    // === Выбор бочки ===
    public static String selectionOutOfRange(int max) {
        return "Выберите от 1 до " + max + " бочек";
    }

    public static String roundNotInProgress(int roundNumber) {
        return "Раунд " + roundNumber + " не идёт";
    }

    public static String barrelNotInRound(UUID barrelId) {
        return "Бочка " + barrelId + " не принадлежит этому раунду";
    }

    // === Ограничение запросов ===
    public static final String JOIN_RATE_LIMITED = "Слишком много попыток входа. Подождите перед следующей попыткой.";

    // === Приз ===
    public static String noWinnerFound(UUID roomId) {
        return "Победитель для комнаты " + roomId + " не найден";
    }
}
