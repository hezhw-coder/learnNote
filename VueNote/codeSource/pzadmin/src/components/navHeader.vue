<template>
    <div class="header-container">
        <div class="header-left flex-box">
            <el-icon class="icon" size="20" @click="store.commit('collapseMenu')">
                <Fold></Fold>
            </el-icon>
            <ul class="flex-box">
                <li v-for="(item, index) in selectMenu" :key="item.path" class="tab flex-box"
                    :class="{ selected: route.path === item.path }">
                    <el-icon size="12">
                        <component :is="item.icon"></component>
                    </el-icon>
                    <RouterLink class="text flex-box" :to="{ path: item.path }">
                        {{ item.name }}
                    </RouterLink>

                    <el-icon class="close" size="12" @click="closeTab(item, index)">
                        <Close></Close>
                    </el-icon>
                </li>
            </ul>
        </div>
        <div class="header-right">
            <el-dropdown>
                <div class="el-dropdown-link flex-box">
                    <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
                    <p class="user-name">admin</p>
                </div>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item>Action 1</el-dropdown-item>
                        <el-dropdown-item>Action 2</el-dropdown-item>
                        <el-dropdown-item>Action 3</el-dropdown-item>
                        <el-dropdown-item disabled>Action 4</el-dropdown-item>
                        <el-dropdown-item divided>Action 5</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
</template>

<script setup>
import { Fold, Close } from '@element-plus/icons-vue';
import { useStore } from 'vuex'
import { computed } from 'vue'
import { it } from 'element-plus/es/locales.mjs';
import { RouterLink, useRoute, useRouter } from 'vue-router';
const store = useStore();

const route = useRoute()
const router = useRouter()

const selectMenu = computed(() => store.state.menu.selectmenu)
const closeTab = (item, index) => {
    store.commit('closeMenu', item)
    if (route.path !== item.path) {
        return;
    }
    const selectMenuData = selectMenu.value;
    //删除的是最后一项
    if (index === selectMenuData.length) {
        if (!selectMenuData.length) {//只有一个，删除后跳转到根目录
            router.push('/')
        } else {//则向前移动一位
            router.push({ path: selectMenuData[index - 1].path })
        }
    } else {//如果删除的是中间位置,则向后移动一位
        router.push({ path: selectMenuData[index].path })
    }
}
</script>

<style lang="less" scoped>
.flex-box {
    height: 100%;
    display: flex;
    align-items: center;
}

.header-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 100%;
    background-color: #fff;
    padding-right: 25px;

    .header-left {
        height: 100%;

        .icon {
            width: 45px;
            height: 100%;
        }

        .icon:hover {
            background-color: #f5f5f5;
            cursor: pointer;
        }

        .tab {
            padding: 0 10px;
            height: 100%;

            .text {
                margin: 0 5px;
            }

            .close {
                visibility: hidden;
            }

            &.selected {
                a {
                    color: #409eff;
                }

                i {
                    color: #409eff;
                }

                background-color: #f5f5f5;
            }
        }

        .tab:hover {
            background-color: #f5f5f5;

            .close {
                visibility: inherit;
                cursor: pointer;
                color: #000;
            }
        }
    }

    .header-right {
        .user-name {
            margin-left: 10px;
        }
    }

    a {
        height: 100%;
        color: #333;
        font-size: 15px;
    }
}
</style>