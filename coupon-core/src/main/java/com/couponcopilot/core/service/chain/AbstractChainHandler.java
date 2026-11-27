package com.couponcopilot.core.service.chain;

import org.springframework.core.Ordered;

/**
 * 责任链处理器接口。实现类通过 Ordered 控制同一业务链内的执行顺序（order 越小越先执行）。
 */
public interface AbstractChainHandler<T> extends Ordered {

    /**
     * 执行当前节点的校验或处理逻辑，校验失败应直接抛出明确的领域异常。
     */
    void handle(T requestParam);

    /**
     * 所属业务链标识，见 {@link ChainBizMark}。
     */
    String mark();
}
