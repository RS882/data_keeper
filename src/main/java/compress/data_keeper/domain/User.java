package compress.data_keeper.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.Setter;
import lombok.AccessLevel;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_name")
    private String userName;

    @OneToMany(mappedBy = "owner")
    private Set<Folder> folderSet = new HashSet<>();
}
