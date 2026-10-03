import SwiftUI

struct ProfileHeader: View {
    let profile: Profile

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.crop.circle.fill")
                .font(.system(size: 48))
                .foregroundStyle(.tint)
            VStack(alignment: .leading, spacing: 4) {
                Text(profile.displayName)
                    .font(.title2)
                    .fontWeight(.bold)
                // ニックネームがあるときだけ本名を添える
                if profile.nickname != nil {
                    Text(profile.name)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.quaternary, in: RoundedRectangle(cornerRadius: 12))
    }
}

#Preview("ニックネームあり") {
    ProfileHeader(profile: Profile(name: "Taro Yamada", nickname: "Taro"))
        .padding()
}

#Preview("ニックネームなし") {
    ProfileHeader(profile: Profile(name: "Hanako Suzuki", nickname: nil))
        .padding()
}
