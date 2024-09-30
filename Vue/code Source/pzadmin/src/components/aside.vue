<template>
    <el-menu :style="{ width: !isCollapse?'230px':'64px' }" active-text-color="#ffd04b" background-color="#545c64" class="aside-container"
        default-active="2" text-color="#fff" @open="handleOpen" @close="handleClose" :collapse=isCollapse>
        <p class="logo-lg">{{isCollapse?'DIDI':'DIDI陪诊'}}</p>
        <Treemenu :menuData="menuData" :index="1"></Treemenu>
    </el-menu>
</template>

<script setup>
// import {
//     Document,
//     Menu as IconMenu,
//     Location,
//     Setting,
// } from '@element-plus/icons-vue'
import Treemenu from './treeMenu.vue'
import { useRouter } from 'vue-router'
import { reactive,computed } from 'vue'
import { useStore } from 'vuex'

const router = useRouter();
const menuData = reactive(router.options.routes[0].children)

const store = useStore()

const isCollapse= computed(() => store.state.menu.isCollapse)

const handleOpen = () => {
}
const handleClose = () => {

}
</script>

<style lang="less" scoped>
.aside-container {
    height: 100%;

    .logo-lg {
        font-size: 20px;
        text-align: center;
        height: 50px;
        line-height: 50px;
        color: #fff;
    }

}
</style>