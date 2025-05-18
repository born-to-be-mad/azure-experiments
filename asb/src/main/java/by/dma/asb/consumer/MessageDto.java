package by.dma.asb.consumer;

import lombok.Data;

@Data
public class MessageDto {
    private String id;
    private String value;
    private int duration;
    private boolean error;
}

