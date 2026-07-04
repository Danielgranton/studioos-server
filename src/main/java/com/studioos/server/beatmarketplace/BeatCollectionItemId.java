package com.studioos.server.beatmarketplace;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeatCollectionItemId implements Serializable {
    private String collectionId;
    private String beatId;
}