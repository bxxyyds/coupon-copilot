package com.couponcopilot.core.service.chain;

import com.couponcopilot.common.exception.ServiceException;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 责任链上下文容器：启动时自动收集所有 AbstractChainHandler 并按 mark 分组、按 order 升序编排。
 */
@Component
public class ChainContext {

    private final Map<String, List<AbstractChainHandler<?>>> chainHandlerMap;

    public ChainContext(List<AbstractChainHandler<?>> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            this.chainHandlerMap = Collections.emptyMap();
            return;
        }
        Map<String, List<AbstractChainHandler<?>>> grouped = handlers.stream()
                .collect(Collectors.groupingBy(
                        AbstractChainHandler::mark,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    list.sort(Comparator.comparingInt(Ordered::getOrder));
                                    return Collections.unmodifiableList(list);
                                }
                        )
                ));
        this.chainHandlerMap = Collections.unmodifiableMap(new HashMap<>(grouped));
    }

    /**
     * 按编排顺序执行指定业务标识下的责任链。
     */
    @SuppressWarnings("unchecked")
    public <T> void execute(String mark, T requestParam) {
        List<AbstractChainHandler<?>> handlers = chainHandlerMap.get(mark);
        if (handlers == null || handlers.isEmpty()) {
            throw new ServiceException("未找到对应的责任链处理器: " + mark);
        }
        for (AbstractChainHandler<?> handler : handlers) {
            ((AbstractChainHandler<T>) handler).handle(requestParam);
        }
    }
}
