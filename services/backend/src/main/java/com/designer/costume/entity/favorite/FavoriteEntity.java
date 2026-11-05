package com.designer.costume.entity.favorite;

import com.designer.costume.entity.common.CreatedAtEntity;
import com.designer.costume.entity.costume.CostumeEntity;
import com.designer.costume.entity.user.UserEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uq_favorites_user_costume", columnNames = {"user_id", "costume_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class FavoriteEntity extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "costume_id", nullable = false)
    @NotNull
    private CostumeEntity costume;
}
