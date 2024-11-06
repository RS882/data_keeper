package compress.data_keeper.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "folder")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Folder extends EntityInfo {

    @Column(name = "is_temp")
    private boolean isTemp = true;

    @Column(name = "is_protected")
    private boolean isProtected = false;

    @OneToMany(mappedBy = "folder")
    private Set<FileInfo> fileInfoSet = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User owner;
}
